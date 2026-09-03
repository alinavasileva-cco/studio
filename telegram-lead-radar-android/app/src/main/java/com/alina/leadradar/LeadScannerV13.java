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
 * v13 scanner: recall-first search for presentation / site / landing requests.
 *
 * Important behaviour:
 *  - reads every public Telegram message available in the selected history window;
 *  - splits digest posts into separate task blocks;
 *  - keeps a relevant task even when a Telegram contact cannot be parsed;
 *  - extracts a direct @username / t.me username when it is available;
 *  - rejects obvious self-promotion, regular employment and excluded site platforms.
 */
public final class LeadScannerV13 {
    private static final Pattern USERNAME = Pattern.compile("^[A-Za-z0-9_]{5,32}$");
    private static final Pattern HANDLE = Pattern.compile("@([A-Za-z0-9_]{5,32})");
    private static final Pattern TME_RAW = Pattern.compile("(?iu)(?:https?://)?(?:t\\.me|telegram\\.me)/([A-Za-z0-9_]{5,32})(?:\\?[^\\s]*)?");
    private static final Pattern BUDGET = Pattern.compile("(?iu)(?:бюджет|оплата|цена|гонорар|ставка)?[^\\d]{0,24}(\\d{1,3}(?:[ \\u00A0]\\d{3})+|\\d{3,7})\\s*(₽|руб(?:лей|ля|\\.)?|р\\b)");

    private static final Pattern NUMBER_LINE = Pattern.compile("(?m)(?=^\\s*\\d{1,3}[.)]\\s+\\S)");
    private static final Pattern NUMBER_INLINE = Pattern.compile("(?iu)(?=(?:^|\\s)\\d{1,3}[.)]\\s+(?:#|[А-ЯA-Z]))");
    private static final Pattern HASH_LINE = Pattern.compile("(?m)(?=^\\s*#[^\\n]{2,100})");
    private static final Pattern BULLET_LINE = Pattern.compile(
            "(?ium)(?=^\\s*[•▪◾●✅☑🔹🔸⭐👉✍📌📝➖—-]+\\s*(?:#|ищ|нуж|треб|задач|сдел|созд|оформ|разраб|дизайн|презентац|сайт|лендинг))"
    );
    private static final Pattern BLANK_PARAGRAPH = Pattern.compile("(?m)(?=\\n\\s*\\n)");

    private static final String[] PRESENTATION_TOPIC = {
            "презентац", "слайд", "powerpoint", "power point", "pptx", "ppt ",
            "pitch deck", "pitch-deck", "питч-дек", "питч дек", "коммерческое предложение в pdf"
    };

    private static final String[] SITE_TOPIC = {
            "лендинг", "landing", "сайт", "website", "web-site", "веб-сайт", "веб сайт",
            "одностраничник", "одностраничный", "сайт-визит", "landing page"
    };

    private static final String[] SITE_PLATFORM_EXCLUDE = {
            "tilda", "тильд", "wordpress", "вордпресс"
    };

    private static final String[] COMPLEX_SITE_EXCLUDE = {
            "интернет-магазин", "интернет магазин", "маркетплейс", "мобильное приложение",
            "личный кабинет", "backend", "бекенд", "fullstack", "фулстек", "saas",
            "crm-система", "crm система", "1с-битрикс", "битрикс"
    };

    private static final String[] SELF_PROMO = {
            "ищу работу", "ищу клиентов", "оказываю услуги", "мои услуги", "предлагаю услуги",
            "делаю сайты", "разрабатываю сайты", "создаю сайты", "сделаю вам сайт",
            "создам вам сайт", "сделаю лендинг", "создам лендинг", "заказать у меня",
            "мой прайс", "мои кейсы", "готов взять заказ", "возьму заказ"
    };

    private static final String[] EMPLOYMENT = {
            "#вакансия", "в штат", "штатный сотрудник", "оформление по тк", "трудоустройство по тк",
            "испытательный срок", "полная занятость", "частичная занятость", "full-time", "full time",
            "фултайм", "график 5/2", "график 2/2", "оклад", "зарплата", "salary",
            "релокация", "собеседование", "рабочий день", "часов в неделю", "ставка в месяц",
            "опыт работы от 2", "опыт работы от 3", "опыт от 2 лет", "опыт от 3 лет"
    };

    public List<Lead> scanChannel(String rawChannel, LeadStore store) throws Exception {
        String channel = normalizeChannel(rawChannel);
        List<Lead> out = new ArrayList<>();
        if (channel.isEmpty()) return out;

        Instant cutoff = Instant.now().minus(Duration.ofDays(store.lookbackDays()));
        Set<String> seenPosts = new HashSet<>();
        Set<String> seenLeads = new HashSet<>();
        int[] stats = new int[4]; // posts, blocks, candidates, noContact

        String before = null;
        Set<Long> anchors = new HashSet<>();
        int pages = 0;

        while (store.running() && pages < 150) {
            String url = "https://t.me/s/" + channel + (before == null ? "" : "?before=" + before);
            Document doc = fetch(url);
            PageState page = processDocument(doc, channel, cutoff, store, seenPosts, seenLeads, out, stats);
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

        store.addDiagnostics(stats[0], stats[1], stats[2], stats[3]);
        return out;
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
                                      Set<String> seenPosts, Set<String> seenLeads,
                                      List<Lead> out, int[] stats) {
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
                // Do not throw away a post only because Telegram omitted its datetime attribute.
                ps.fresh++;
            }

            String postUrl = "https://t.me/" + dataPost;
            if (!seenPosts.add(postUrl)) continue;
            stats[0]++;

            String full = extractFullText(message, channel);
            if (full.length() < 5) continue;

            List<TaskBlock> blocks = splitTasks(full);
            stats[1] += blocks.size();
            List<ContactRef> contacts = collectContacts(full, channel);

            boolean anyBlockCandidate = false;
            for (TaskBlock block : blocks) {
                String text = clean(block.text);
                if (text.length() < 5) continue;

                Lead.Category category = classify(text, store);
                if (category == null) continue;
                anyBlockCandidate = true;
                stats[2]++;

                String user = chooseContact(full, block, contacts);
                if (user == null) user = "";
                if (user.isEmpty()) stats[3]++;

                String key = category.name() + "|" + postUrl + "|"
                        + Integer.toHexString(normalize(text).hashCode());
                if (!seenLeads.add(key)) continue;

                out.add(new Lead(category, channel, postUrl, text, user, extractBudget(text), key));
            }

            // Safety net for unusual formatting: never lose a topic merely because splitting failed.
            if (!anyBlockCandidate) {
                String whole = clean(full);
                Lead.Category category = classify(whole, store);
                if (category != null) {
                    stats[2]++;
                    TaskBlock parent = new TaskBlock(whole, 0, full.length());
                    String user = chooseContact(full, parent, contacts);
                    if (user == null) user = "";
                    if (user.isEmpty()) stats[3]++;

                    String key = category.name() + "|" + postUrl + "|whole|"
                            + Integer.toHexString(normalize(whole).hashCode());
                    if (seenLeads.add(key)) {
                        out.add(new Lead(category, channel, postUrl, whole, user, extractBudget(whole), key));
                    }
                }
            }
        }
        return ps;
    }

    private String extractFullText(Element message, String channel) {
        Element textNode = message.selectFirst("div.tgme_widget_message_text");
        if (textNode == null) textNode = message.selectFirst("[class*=message_text]");
        if (textNode == null) textNode = message;

        Element copy = textNode.clone();
        for (Element a : copy.select("a[href]")) {
            String u = usernameFromHref(a.attr("href"));
            if (!allowed(u, channel)) continue;
            String visible = a.text() == null ? "" : a.text();
            if (!visible.toLowerCase(Locale.ROOT).contains(u.toLowerCase(Locale.ROOT))) {
                a.after(" @" + u);
            }
        }

        String text = copy.wholeText().trim();
        if (text.isEmpty()) text = copy.text().trim();
        text = exposeRawTelegramUrls(text, channel);

        String whole = message.wholeText() == null ? "" : message.wholeText().trim();
        whole = exposeRawTelegramUrls(whole, channel);

        Set<String> textUsers = collectHandles(text, channel);
        Set<String> wholeUsers = collectHandles(whole, channel);
        if (textUsers.isEmpty() && !wholeUsers.isEmpty() && whole.length() >= text.length()) {
            text = whole;
        }

        // Hidden t.me anchors may not be present in visible text at all.
        Set<String> anchorUsers = new HashSet<>();
        for (Element a : message.select("a[href]")) {
            String u = usernameFromHref(a.attr("href"));
            if (allowed(u, channel)) anchorUsers.add(u);
        }
        Set<String> existing = collectHandles(text, channel);
        anchorUsers.removeAll(existing);
        if (anchorUsers.size() == 1) {
            text += "\nКонтакт: @" + anchorUsers.iterator().next();
        }
        return clean(text);
    }

    private List<TaskBlock> splitTasks(String text) {
        TreeSet<Integer> starts = new TreeSet<>();
        starts.add(0);
        collectStarts(text, NUMBER_LINE, starts);
        collectStarts(text, NUMBER_INLINE, starts);
        collectStarts(text, HASH_LINE, starts);
        collectStarts(text, BULLET_LINE, starts);
        collectStarts(text, BLANK_PARAGRAPH, starts);

        List<Integer> points = new ArrayList<>(starts);
        List<TaskBlock> blocks = new ArrayList<>();
        for (int i = 0; i < points.size(); i++) {
            int from = points.get(i);
            int to = i + 1 < points.size() ? points.get(i + 1) : text.length();
            if (to <= from) continue;
            String part = text.substring(from, to).trim();
            if (part.length() >= 5) blocks.add(new TaskBlock(part, from, to));
        }

        if (blocks.isEmpty()) blocks.add(new TaskBlock(text, 0, text.length()));
        return mergeTiny(blocks);
    }

    private List<TaskBlock> mergeTiny(List<TaskBlock> input) {
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
        if (containsAny(s, SELF_PROMO)) return null;
        if (isClearEmployment(s)) return null;

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
        // A single #вакансия tag is not enough: many freelance channels use it for one-off tasks.
        if (s.contains("в штат") || s.contains("оформление по тк") || s.contains("трудоустройство по тк")) {
            return true;
        }
        int score = 0;
        for (String x : EMPLOYMENT) if (s.contains(x)) score++;
        return score >= 2;
    }

    private boolean hasTopic(String text) {
        String s = text.toLowerCase(Locale.ROOT);
        return containsAny(s, PRESENTATION_TOPIC) || containsAny(s, SITE_TOPIC);
    }

    private List<ContactRef> collectContacts(String full, String channel) {
        List<ContactRef> refs = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        Matcher h = HANDLE.matcher(full);
        while (h.find()) {
            String u = h.group(1);
            if (!allowed(u, channel)) continue;
            String k = u.toLowerCase(Locale.ROOT) + "@" + h.start();
            if (seen.add(k)) refs.add(new ContactRef(u, h.start(), h.end()));
        }

        Matcher raw = TME_RAW.matcher(full);
        while (raw.find()) {
            String u = raw.group(1);
            if (!allowed(u, channel)) continue;
            String k = u.toLowerCase(Locale.ROOT) + "@" + raw.start();
            if (seen.add(k)) refs.add(new ContactRef(u, raw.start(), raw.end()));
        }
        return refs;
    }

    private String chooseContact(String full, TaskBlock block, List<ContactRef> refs) {
        if (refs.isEmpty()) return "";

        ContactRef bestInside = null;
        int bestInsideDistance = Integer.MAX_VALUE;
        for (ContactRef r : refs) {
            if (r.start >= block.start && r.start <= block.end + 8) {
                int d = Math.abs(block.end - r.start);
                if (d < bestInsideDistance) {
                    bestInside = r;
                    bestInsideDistance = d;
                }
            }
        }
        if (bestInside != null) return bestInside.username;
        if (refs.size() == 1) return refs.get(0).username;

        ContactRef best = null;
        int bestScore = Integer.MAX_VALUE;
        for (ContactRef r : refs) {
            int distance;
            if (r.start > block.end) {
                distance = r.start - block.end;
                if (distance > 500) continue;
            } else if (r.end < block.start) {
                distance = block.start - r.end + 150; // prefer a contact following the task
                if (distance > 320) continue;
            } else {
                distance = 0;
            }

            int from = Math.max(0, r.start - 70);
            int to = Math.min(full.length(), r.end + 50);
            String around = full.substring(from, to).toLowerCase(Locale.ROOT);
            if (around.contains("подпис") || around.contains("наш канал") || around.contains("канал @")) {
                distance += 700;
            }
            if (distance < bestScore) {
                bestScore = distance;
                best = r;
            }
        }
        return best == null ? "" : best.username;
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
            boolean hasPostPath = after < text.length() && text.charAt(after) == '/';
            if (allowed(u, channel) && !hasPostPath) repl += " @" + u;
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
        if (p < 0) {
            p = lower.indexOf("telegram.me/");
            len = 12;
        }
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

    private long parsePostId(String dataPost) {
        int slash = dataPost.lastIndexOf('/');
        if (slash < 0) return -1L;
        try { return Long.parseLong(dataPost.substring(slash + 1)); }
        catch (Exception e) { return -1L; }
    }

    private void collectStarts(String text, Pattern pattern, Set<Integer> starts) {
        Matcher m = pattern.matcher(text);
        while (m.find()) starts.add(m.start());
    }

    private String clean(String text) {
        if (text == null) return "";
        return text.replaceAll("[\\t ]+", " ")
                .replaceAll("\\n{3,}", "\\n\\n")
                .trim();
    }

    private String normalize(String text) {
        if (text == null) return "";
        return text.toLowerCase(Locale.ROOT)
                .replaceAll("https?://\\S+", " ")
                .replaceAll("@[A-Za-z0-9_]{5,32}", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private boolean containsAny(String s, String... values) {
        for (String v : values) if (v != null && !v.isEmpty() && s.contains(v)) return true;
        return false;
    }

    public static String normalizeChannel(String raw) {
        if (raw == null) return "";
        String s = raw.trim();
        if (s.startsWith("https://t.me/s/")) s = s.substring("https://t.me/s/".length());
        else if (s.startsWith("http://t.me/s/")) s = s.substring("http://t.me/s/".length());
        else if (s.startsWith("https://t.me/")) s = s.substring("https://t.me/".length());
        else if (s.startsWith("http://t.me/")) s = s.substring("http://t.me/".length());
        while (s.startsWith("@") || s.startsWith("/")) s = s.substring(1);
        int q = s.indexOf('?'); if (q >= 0) s = s.substring(0, q);
        int slash = s.indexOf('/'); if (slash >= 0) s = s.substring(0, slash);
        return s.matches("[A-Za-z0-9_]{4,64}") ? s : "";
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

    private static final class PageState {
        int messages = 0;
        int dated = 0;
        int fresh = 0;
        long minPostId = Long.MAX_VALUE;
    }
}
