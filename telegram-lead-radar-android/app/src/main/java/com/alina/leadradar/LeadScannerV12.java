package com.alina.leadradar;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.net.URLDecoder;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * v13 search core.
 * Reads the full public history for the chosen period first, then detects topic blocks.
 * The topic filter is intentionally recall-first: any genuine task block mentioning
 * presentation/slides or site/landing is kept, while obvious employment and self-promo are rejected.
 * Telegram contacts are bound by their real position in the parent post, not by mandatory words
 * such as "пишите" or "отклик".
 */
public final class LeadScannerV12 {
    private static final Pattern USERNAME = Pattern.compile("^[A-Za-z0-9_]{5,32}$");
    private static final Pattern HANDLE = Pattern.compile("@([A-Za-z0-9_]{5,32})");
    private static final Pattern TME_RAW = Pattern.compile("(?iu)(?:https?://)?(?:t\\.me|telegram\\.me)/([A-Za-z0-9_]{5,32})(?:\\?[^\\s]*)?");
    private static final Pattern BUDGET = Pattern.compile("(?iu)(?:бюджет|оплата|цена|гонорар|ставка)?[^\\d]{0,24}(\\d{1,3}(?:[ \\u00A0]\\d{3})+|\\d{3,7})\\s*(₽|руб(?:лей|ля|\\.)?|р\\b)");

    private static final Pattern NUMBER_START = Pattern.compile("(?m)(?=^\\s*\\d{1,3}[.)]\\s+\\S)");
    private static final Pattern NUMBER_INLINE = Pattern.compile("(?iu)(?=(?:^|\\s)\\d{1,3}[.)]\\s+#\\S)");
    private static final Pattern HASH_START = Pattern.compile("(?m)(?=^\\s*#[^\\n]{2,80})");
    private static final Pattern TASK_BULLET_START = Pattern.compile(
            "(?ium)(?=^\\s*[•▪◾●✅☑🔹🔸⭐👉✍📌📝➖—-]+\\s*(?:#|ищ|нуж|треб|задач|сдел|созд|оформ|разраб|дизайн|презентац|сайт|лендинг))"
    );

    private static final String[] SELF_PROMO = {
            "ищу работу", "ищу клиентов", "оказываю услуги", "мои услуги", "предлагаю услуги",
            "создам вам", "сделаю вам", "делаю сайты", "разрабатываю сайты", "создаю сайты",
            "сделаю лендинг", "создам лендинг", "заказать у меня", "мой прайс", "мои кейсы"
    };

    private static final String[] HARD_EMPLOYMENT = {
            "#вакансия", "в штат", "штатный сотрудник", "оформление по тк", "трудоустройство по тк",
            "испытательный срок", "полная занятость", "частичная занятость", "full-time", "full time",
            "фултайм", "график 5/2", "график 2/2", "оклад", "зарплата", "salary", "релокация",
            "собеседование", "рабочий день", "часов в неделю", "ставка в месяц"
    };

    private static final String[] PRESENTATION_TOPIC = {
            "презентац", "слайд", "powerpoint", "power point", "pptx", "ppt ", "pitch deck", "питч-дек", "питч дек"
    };

    private static final String[] SITE_TOPIC = {
            "лендинг", "landing", "сайт", "website", "web-site", "веб-сайт", "веб сайт", "одностраничник", "сайт-визит"
    };

    private static final String[] SITE_PLATFORM_EXCLUDE = {
            "tilda", "тильд", "wordpress", "вордпресс"
    };

    private static final String[] COMPLEX_SITE_EXCLUDE = {
            "интернет-магазин", "интернет магазин", "маркетплейс", "мобильное приложение", "личный кабинет",
            "backend", "бекенд", "fullstack", "фулстек", "saas", "crm-система", "crm система"
    };

    public List<Lead> scanChannel(String rawChannel, LeadStore store) throws Exception {
        String channel = normalizeChannel(rawChannel);
        List<Lead> out = new ArrayList<>();
        if (channel.isEmpty()) return out;

        Instant cutoff = Instant.now().minus(Duration.ofDays(store.lookbackDays()));
        Set<String> seenPosts = new HashSet<>();
        int[] stats = new int[4]; // posts, blocks, topic candidates, no contact
        scanFullHistory(channel, cutoff, store, seenPosts, out, stats);
        store.addDiagnostics(stats[0], stats[1], stats[2], stats[3]);
        return out;
    }

    private void scanFullHistory(String channel, Instant cutoff, LeadStore store, Set<String> seenPosts,
                                 List<Lead> out, int[] stats) throws Exception {
        String before = null;
        Set<Long> anchors = new HashSet<>();
        int pages = 0;

        while (store.running() && pages < 120) {
            String url = "https://t.me/s/" + channel + (before == null ? "" : "?before=" + before);
            Document doc = fetch(url);
            PageState page = processDocument(doc, channel, cutoff, store, seenPosts, out, stats);
            pages++;

            if (page.messages == 0) break;
            if (page.dated > 0 && page.fresh == 0) break;
            if (page.minPostId == Long.MAX_VALUE || page.minPostId <= 1) break;
            if (!anchors.add(page.minPostId)) break;

            before = String.valueOf(page.minPostId);
            try { Thread.sleep(90L); }
            catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private Document fetch(String url) throws Exception {
        Connection.Response r = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 Chrome/131 Mobile Safari/537.36")
                .header("Accept-Language", "ru-RU,ru;q=0.9,en;q=0.7")
                .followRedirects(true)
                .ignoreHttpErrors(true)
                .timeout(25000)
                .execute();
        if (r.statusCode() < 200 || r.statusCode() >= 400) {
            throw new IllegalStateException("HTTP " + r.statusCode());
        }
        return r.parse();
    }

    private PageState processDocument(Document doc, String channel, Instant cutoff, LeadStore store,
                                      Set<String> seenPosts, List<Lead> out, int[] stats) {
        Elements messages = doc.select("div.tgme_widget_message[data-post]");
        if (messages.isEmpty()) messages = doc.select("[data-post]");

        PageState ps = new PageState();
        ps.messages = messages.size();

        for (Element message : messages) {
            if (!store.running()) break;

            String dataPost = message.attr("data-post");
            if (dataPost == null || dataPost.isEmpty()) continue;

            long postId = parsePostId(dataPost);
            if (postId > 0 && postId < ps.minPostId) ps.minPostId = postId;

            Instant when = parsePostedAt(message);
            if (when != null) {
                ps.dated++;
                if (when.isBefore(cutoff)) continue;
                ps.fresh++;
            } else {
                // Telegram normally exposes time; if not, do not silently drop the post.
                ps.fresh++;
            }

            String postUrl = "https://t.me/" + dataPost;
            if (!seenPosts.add(postUrl)) continue;
            stats[0]++;

            Element textNode = message.selectFirst("div.tgme_widget_message_text");
            if (textNode == null) textNode = message.selectFirst("[class*=message_text]");
            if (textNode == null) textNode = message;

            String full = textWithVisibleContacts(textNode, message, channel);
            if (full.trim().length() < 8) continue;

            List<TaskBlock> blocks = splitTasks(full);
            stats[1] += blocks.size();
            List<ContactRef> contacts = collectContactRefs(full, channel);

            boolean foundTopicBlock = false;
            for (TaskBlock block : blocks) {
                String text = clean(block.text);
                Lead.Category category = classify(text, store);
                if (category == null) continue;
                foundTopicBlock = true;
                stats[2]++;

                String user = chooseNearestContact(full, block, contacts, channel);
                if (user == null || user.isEmpty()) {
                    stats[3]++;
                    continue;
                }

                String key = category.name() + "|" + user.toLowerCase(Locale.ROOT)
                        + "|" + Integer.toHexString(normalize(text).hashCode());
                out.add(new Lead(category, channel, postUrl, text, user, extractBudget(text), key));
            }

            // Safety net: if segmentation failed but the parent post clearly contains one of our topics,
            // evaluate the whole post once instead of losing the request entirely.
            if (!foundTopicBlock) {
                String whole = clean(full);
                Lead.Category category = classify(whole, store);
                if (category != null) {
                    stats[2]++;
                    TaskBlock parent = new TaskBlock(whole, 0, full.length());
                    String user = chooseNearestContact(full, parent, contacts, channel);
                    if (user == null || user.isEmpty()) {
                        stats[3]++;
                    } else {
                        String key = category.name() + "|" + user.toLowerCase(Locale.ROOT)
                                + "|" + Integer.toHexString(normalize(whole).hashCode());
                        out.add(new Lead(category, channel, postUrl, whole, user, extractBudget(whole), key));
                    }
                }
            }
        }
        return ps;
    }

    private String textWithVisibleContacts(Element textNode, Element message, String channel) {
        Element copy = textNode.clone();
        for (Element a : copy.select("a[href]")) {
            String user = usernameFromHref(a.attr("href"));
            if (!allowed(user, channel)) continue;
            String visible = a.text() == null ? "" : a.text();
            if (!visible.toLowerCase(Locale.ROOT).contains(user.toLowerCase(Locale.ROOT))) {
                a.after(" @" + user);
            }
        }

        String text = copy.wholeText().trim();
        if (text.isEmpty()) text = copy.text().trim();
        text = exposeRawTelegramUrls(text, channel);

        // Some Telegram templates keep the contact outside message_text. Preserve it.
        String whole = message.wholeText() == null ? "" : message.wholeText().trim();
        whole = exposeRawTelegramUrls(whole, channel);
        Set<String> inText = collectHandles(text, channel);
        Set<String> inWhole = collectHandles(whole, channel);
        if (inText.isEmpty() && !inWhole.isEmpty() && whole.length() >= text.length()) {
            text = whole;
        }

        // Hidden anchors may not be represented in wholeText at all.
        Set<String> anchors = new HashSet<>();
        for (Element a : message.select("a[href]")) {
            String u = usernameFromHref(a.attr("href"));
            if (allowed(u, channel)) anchors.add(u);
        }
        Set<String> already = collectHandles(text, channel);
        anchors.removeAll(already);
        if (anchors.size() == 1) {
            text += "\n@" + anchors.iterator().next();
        }
        return text;
    }

    private List<TaskBlock> splitTasks(String text) {
        TreeSet<Integer> starts = new TreeSet<>();
        starts.add(0);
        collectStarts(text, NUMBER_START, starts);
        collectStarts(text, NUMBER_INLINE, starts);
        collectStarts(text, HASH_START, starts);
        collectStarts(text, TASK_BULLET_START, starts);

        List<Integer> points = new ArrayList<>(starts);
        List<TaskBlock> blocks = new ArrayList<>();
        for (int i = 0; i < points.size(); i++) {
            int from = points.get(i);
            int to = i + 1 < points.size() ? points.get(i + 1) : text.length();
            if (to <= from) continue;
            String part = text.substring(from, to).trim();
            if (part.length() >= 8) blocks.add(new TaskBlock(part, from, to));
        }

        if (blocks.isEmpty()) blocks.add(new TaskBlock(text, 0, text.length()));
        return mergeTinyFragments(blocks);
    }

    private List<TaskBlock> mergeTinyFragments(List<TaskBlock> input) {
        List<TaskBlock> out = new ArrayList<>();
        for (TaskBlock b : input) {
            if (!out.isEmpty() && b.text.length() < 18 && !hasTopic(b.text)) {
                TaskBlock prev = out.remove(out.size() - 1);
                out.add(new TaskBlock(prev.text + "\n" + b.text, prev.start, b.end));
            } else {
                out.add(b);
            }
        }
        return out;
    }

    private Lead.Category classify(String text, LeadStore store) {
        String s = text.toLowerCase(Locale.ROOT);
        if (countCyrillic(s) < 3) return null;
        if (containsAny(s, SELF_PROMO)) return null;
        if (isClearEmployment(s)) return null;

        // Presentation detection is intentionally broad: the user manually selects final leads.
        if (store.presentationsEnabled() && containsAny(s, PRESENTATION_TOPIC)) {
            return Lead.Category.PRESENTATION;
        }

        if (store.sitesEnabled() && containsAny(s, SITE_TOPIC)) {
            if (containsAny(s, SITE_PLATFORM_EXCLUDE)) return null;
            if (containsAny(s, COMPLEX_SITE_EXCLUDE)) return null;
            return Lead.Category.SITE;
        }
        return null;
    }

    private boolean isClearEmployment(String s) {
        int hard = 0;
        for (String x : HARD_EMPLOYMENT) if (s.contains(x)) hard++;
        if (s.contains("#вакансия") || s.contains("в штат") || s.contains("оформление по тк")) return true;
        // Do not reject a freelance request merely because it says "требуется дизайнер".
        return hard >= 2;
    }

    private boolean hasTopic(String text) {
        String s = text.toLowerCase(Locale.ROOT);
        return containsAny(s, PRESENTATION_TOPIC) || containsAny(s, SITE_TOPIC);
    }

    private List<ContactRef> collectContactRefs(String full, String channel) {
        List<ContactRef> refs = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        Matcher h = HANDLE.matcher(full);
        while (h.find()) {
            String u = h.group(1);
            if (!allowed(u, channel)) continue;
            String key = u.toLowerCase(Locale.ROOT) + "@" + h.start();
            if (seen.add(key)) refs.add(new ContactRef(u, h.start(), h.end()));
        }

        Matcher raw = TME_RAW.matcher(full);
        while (raw.find()) {
            String u = raw.group(1);
            if (!allowed(u, channel)) continue;
            String key = u.toLowerCase(Locale.ROOT) + "@" + raw.start();
            if (seen.add(key)) refs.add(new ContactRef(u, raw.start(), raw.end()));
        }
        return refs;
    }

    private String chooseNearestContact(String full, TaskBlock block, List<ContactRef> refs, String channel) {
        if (refs.isEmpty()) return null;

        List<ContactRef> inside = new ArrayList<>();
        for (ContactRef r : refs) {
            if (r.start >= block.start && r.start <= block.end + 4) inside.add(r);
        }
        if (inside.size() == 1) return inside.get(0).username;
        if (inside.size() > 1) {
            // In a task block, the contact nearest its end is usually the response contact.
            ContactRef best = inside.get(0);
            int bestDistance = Math.abs(block.end - best.start);
            for (ContactRef r : inside) {
                int d = Math.abs(block.end - r.start);
                if (d < bestDistance) { best = r; bestDistance = d; }
            }
            return best.username;
        }

        if (refs.size() == 1) return refs.get(0).username;

        ContactRef best = null;
        int bestScore = Integer.MAX_VALUE;
        for (ContactRef r : refs) {
            int distance;
            if (r.start > block.end) {
                distance = r.start - block.end;
                if (distance > 420) continue;
            } else if (r.end < block.start) {
                distance = (block.start - r.end) + 120; // prefer following contact over preceding one
                if (distance > 260) continue;
            } else {
                distance = 0;
            }

            int contextFrom = Math.max(0, r.start - 60);
            int contextTo = Math.min(full.length(), r.end + 40);
            String around = full.substring(contextFrom, contextTo).toLowerCase(Locale.ROOT);
            if (around.contains("подпис") || around.contains("наш канал") || around.contains("канал @")) distance += 500;
            if (distance < bestScore) { best = r; bestScore = distance; }
        }
        return best == null ? null : best.username;
    }

    private Set<String> collectHandles(String text, String channel) {
        Set<String> users = new HashSet<>();
        if (text == null) return users;
        Matcher h = HANDLE.matcher(text);
        while (h.find()) if (allowed(h.group(1), channel)) users.add(h.group(1));
        Matcher raw = TME_RAW.matcher(text);
        while (raw.find()) if (allowed(raw.group(1), channel)) users.add(raw.group(1));
        return users;
    }

    private String exposeRawTelegramUrls(String text, String channel) {
        if (text == null || text.isEmpty()) return "";
        Matcher m = TME_RAW.matcher(text);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String u = m.group(1);
            String repl = m.group(0);
            int after = m.end();
            boolean postPath = after < text.length() && text.charAt(after) == '/';
            if (allowed(u, channel) && !postPath) repl += " @" + u;
            m.appendReplacement(sb, Matcher.quoteReplacement(repl));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private boolean allowed(String u, String channel) {
        if (u == null || !USERNAME.matcher(u).matches()) return false;
        if (u.equalsIgnoreCase(channel)) return false;
        String l = u.toLowerCase(Locale.ROOT);
        return !l.endsWith("bot") && !l.contains("kwork") && !l.contains("youdo");
    }

    private String usernameFromHref(String href) {
        if (href == null || href.trim().isEmpty()) return null;
        String h = href.trim();
        String lower = h.toLowerCase(Locale.ROOT);

        if (lower.startsWith("tg://resolve?")) {
            int p = lower.indexOf("domain=");
            if (p < 0) return null;
            String u = h.substring(p + 7);
            int amp = u.indexOf('&');
            if (amp >= 0) u = u.substring(0, amp);
            try { u = URLDecoder.decode(u, "UTF-8"); } catch (Exception ignored) {}
            while (u.startsWith("@")) u = u.substring(1);
            return USERNAME.matcher(u).matches() ? u : null;
        }

        int p = lower.indexOf("t.me/");
        int len = 5;
        if (p < 0) { p = lower.indexOf("telegram.me/"); len = 12; }
        if (p < 0) return null;

        String path = h.substring(p + len);
        int q = path.indexOf('?'); if (q >= 0) path = path.substring(0, q);
        int hash = path.indexOf('#'); if (hash >= 0) path = path.substring(0, hash);
        while (path.startsWith("/")) path = path.substring(1);
        if (path.startsWith("s/") || path.startsWith("c/") || path.startsWith("+")
                || path.startsWith("joinchat/") || path.contains("/")) return null;
        while (path.startsWith("@")) path = path.substring(1);
        return USERNAME.matcher(path).matches() ? path : null;
    }

    private String extractBudget(String text) {
        Matcher m = BUDGET.matcher(text);
        return m.find() ? (m.group(1) + " " + m.group(2)).replace('\u00A0', ' ').trim() : "";
    }

    private Instant parsePostedAt(Element message) {
        Element t = message.selectFirst("time[datetime]");
        if (t == null) return null;
        String raw = t.attr("datetime");
        try { return OffsetDateTime.parse(raw).toInstant(); }
        catch (Exception e) {
            try { return Instant.parse(raw); }
            catch (Exception ignored) { return null; }
        }
    }

    private long parsePostId(String data) {
        int slash = data.lastIndexOf('/');
        if (slash < 0) return -1;
        try { return Long.parseLong(data.substring(slash + 1)); }
        catch (Exception e) { return -1; }
    }

    private String clean(String text) {
        return text.replaceAll("[\\t ]+", " ").replaceAll("\\n{3,}", "\\n\\n").trim();
    }

    private String normalize(String text) {
        return text.toLowerCase(Locale.ROOT)
                .replaceAll("https?://\\S+", " ")
                .replaceAll("@[A-Za-z0-9_]{5,32}", " ")
                .replaceAll("\\s+", " ").trim();
    }

    private void collectStarts(String text, Pattern pattern, Set<Integer> starts) {
        Matcher m = pattern.matcher(text);
        while (m.find()) starts.add(m.start());
    }

    private boolean containsAny(String s, String... needles) {
        for (String x : needles) if (x != null && !x.isEmpty() && s.contains(x)) return true;
        return false;
    }

    private int countCyrillic(String s) {
        int n = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if ((c >= 'А' && c <= 'я') || c == 'ё' || c == 'Ё') n++;
        }
        return n;
    }

    public static String normalizeChannel(String raw) {
        if (raw == null) return "";
        String s = raw.trim();
        s = s.replace("https://t.me/s/", "").replace("http://t.me/s/", "")
                .replace("https://t.me/", "").replace("http://t.me/", "");
        while (s.startsWith("@")) s = s.substring(1);
        int slash = s.indexOf('/'); if (slash >= 0) s = s.substring(0, slash);
        int q = s.indexOf('?'); if (q >= 0) s = s.substring(0, q);
        return USERNAME.matcher(s).matches() ? s : "";
    }

    private static final class PageState {
        int messages;
        int dated;
        int fresh;
        long minPostId = Long.MAX_VALUE;
    }

    private static final class TaskBlock {
        final String text;
        final int start;
        final int end;
        TaskBlock(String text, int start, int end) {
            this.text = text;
            this.start = start;
            this.end = end;
        }
    }

    private static final class ContactRef {
        final String username;
        final int start;
        final int end;
        ContactRef(String username, int start, int end) {
            this.username = username;
            this.start = start;
            this.end = end;
        }
    }
}
