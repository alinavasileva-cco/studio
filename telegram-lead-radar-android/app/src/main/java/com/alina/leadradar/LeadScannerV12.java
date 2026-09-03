package com.alina.leadradar;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.net.URLDecoder;
import java.net.URLEncoder;
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

/** v12: public Telegram reader with digest-aware contact binding. */
public final class LeadScannerV12 {
    private static final Pattern HANDLE = Pattern.compile("@([A-Za-z0-9_]{5,32})");
    private static final Pattern USERNAME = Pattern.compile("^[A-Za-z0-9_]{5,32}$");
    private static final Pattern TME_RAW = Pattern.compile("(?iu)(?:https?://)?(?:t\\.me|telegram\\.me)/([A-Za-z0-9_]{5,32})(?:\\?[^\\s]*)?");
    private static final Pattern BUDGET = Pattern.compile("(?iu)(?:бюджет|оплата|цена)?[^\\d]{0,20}(\\d{1,3}(?:[ \\u00A0]\\d{3})+|\\d{3,7})\\s*(₽|руб(?:лей|ля|\\.)?|р\\b)");
    private static final Pattern NUMBER_START = Pattern.compile("(?m)(?=^\\s*\\d{1,3}[.)]\\s+\\S)");
    private static final Pattern NUMBER_INLINE = Pattern.compile("(?iu)(?=(?:^|\\s)\\d{1,3}[.)]\\s+#)");
    private static final Pattern HASH_START = Pattern.compile("(?ium)(?=^\\s*#(?:дизайнер|презентац|сайт|лендинг|веб|web|заказ|задач)[^\\n]*)");
    private static final Pattern BULLET_START = Pattern.compile("(?ium)(?=^\\s*[•▪◾●✅☑🔹🔸⭐👉✍📌📝]+\\s*(?:#|ищ|нуж|треб|задач|сдел|созд|оформ|разраб))");

    private static final String[] HARD_EMPLOYMENT = {
            "#вакансия","в штат","штатный","оформление по тк","трудоустройство","испытательный срок",
            "full-time","full time","фултайм","part-time","part time","полная занятость","частичная занятость",
            "график 5/2","5/2","оклад","зарплата","salary","грейд","релокация","собеседование",
            "присылайте резюме","отправляйте резюме","ставка в месяц","часов в неделю","рабочий день"
    };
    private static final String[] JOB_ROLE = {
            "product manager","product owner","project manager","проджект менеджер","проектный менеджер",
            "менеджер проектов","менеджер по продажам","sales manager","account manager","ui/ux designer","ux/ui designer",
            "middle designer","senior designer","outstaff","аутстафф"
    };
    private static final String[] SELF_PROMO = {
            "ищу работу","ищу клиентов","оказываю услуги","мои услуги","создам вам","делаю сайты",
            "могу сделать сайт","разрабатываю сайты","создаю сайты","заказать у меня"
    };
    private static final String[] SITE_BUILD = {
            "сделать сайт","создать сайт","разработать сайт","собрать сайт","сверстать сайт","создание сайта","разработка сайта",
            "сайт с нуля","сайт под ключ","нужен сайт","нужно сделать сайт","сделать лендинг","создать лендинг",
            "разработать лендинг","собрать лендинг","сверстать лендинг","создание лендинга","разработка лендинга",
            "лендинг с нуля","лендинг под ключ","нужен лендинг","нужно сделать лендинг","одностраничный сайт","сайт-визит"
    };
    private static final String[] PRESENTATION_BUILD = {
            "сделать презентац","создать презентац","оформить презентац","собрать презентац","подготовить презентац",
            "разработать презентац","переделать презентац","переработать презентац","редизайн презентац","задизайнить презентац",
            "дизайн презентац","верстка презентац","вёрстка презентац","оформление презентац","пересобрать презентац",
            "собрать слайды","оформить слайды","сделать слайды","дизайн слайдов","оформление слайдов",
            "ищу дизайнера презентац","ищем дизайнера презентац","нужен дизайнер презентац","требуется дизайнер презентац",
            "нужна презентация","нужно сделать презентац","заказ на презентац"
    };
    private static final String[] RESPONSE_WORDS = {
            "пишите","писать","напишите","отклик","контакт","связь","связаться","телеграм","telegram","тг",
            "личку","лс","присылайте","портфолио","обращаться","обращайтесь","сюда","в личные"
    };

    public List<Lead> scanChannel(String rawChannel, LeadStore store) throws Exception {
        String channel = normalizeChannel(rawChannel);
        List<Lead> out = new ArrayList<>();
        if (channel.isEmpty()) return out;
        Instant cutoff = Instant.now().minus(Duration.ofDays(store.lookbackDays()));
        Set<String> seen = new HashSet<>();
        int[] stats = new int[4];

        scanHistory(channel, cutoff, store, seen, out, stats);
        String[] queries = {"презентац", "PowerPoint", "лендинг", "сайт"};
        for (String q : queries) {
            if (!store.running()) break;
            try {
                String url = "https://t.me/s/" + channel + "?q=" + URLEncoder.encode(q, "UTF-8");
                Document doc = fetch(url);
                processDocument(doc, channel, cutoff, store, seen, out, stats, false);
            } catch (Exception ignored) {}
        }
        store.addDiagnostics(stats[0], stats[1], stats[2], stats[3]);
        return out;
    }

    private void scanHistory(String channel, Instant cutoff, LeadStore store, Set<String> seen,
                             List<Lead> out, int[] stats) throws Exception {
        String before = null;
        Set<Long> anchors = new HashSet<>();
        while (store.running()) {
            String url = "https://t.me/s/" + channel + (before == null ? "" : "?before=" + before);
            Document doc = fetch(url);
            PageState page = processDocument(doc, channel, cutoff, store, seen, out, stats, true);
            if (page.messages == 0) break;
            if (page.dated > 0 && page.fresh == 0) break;
            if (page.minPostId == Long.MAX_VALUE || page.minPostId <= 1 || !anchors.add(page.minPostId)) break;
            before = String.valueOf(page.minPostId);
            try { Thread.sleep(120L); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); break; }
        }
    }

    private Document fetch(String url) throws Exception {
        Connection.Response r = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 Chrome/131 Mobile Safari/537.36")
                .header("Accept-Language", "ru-RU,ru;q=0.9,en;q=0.7")
                .followRedirects(true).ignoreHttpErrors(true).timeout(25000).execute();
        if (r.statusCode() < 200 || r.statusCode() >= 400) throw new IllegalStateException("HTTP " + r.statusCode());
        return r.parse();
    }

    private PageState processDocument(Document doc, String channel, Instant cutoff, LeadStore store,
                                      Set<String> seen, List<Lead> out, int[] stats, boolean history) {
        Elements messages = doc.select("div.tgme_widget_message[data-post]");
        if (messages.isEmpty()) messages = doc.select("[data-post]");
        PageState ps = new PageState();
        ps.messages = messages.size();

        for (Element message : messages) {
            if (!store.running()) break;
            String dataPost = message.attr("data-post");
            if (dataPost == null || dataPost.isEmpty()) continue;
            long id = parsePostId(dataPost);
            if (id > 0 && id < ps.minPostId) ps.minPostId = id;

            Instant when = parsePostedAt(message);
            if (when != null) {
                ps.dated++;
                if (when.isBefore(cutoff)) continue;
                ps.fresh++;
            } else if (history) ps.fresh++;

            String postUrl = "https://t.me/" + dataPost;
            if (!seen.add(postUrl)) continue;
            stats[0]++;

            Element textNode = message.selectFirst("div.tgme_widget_message_text");
            if (textNode == null) textNode = message.selectFirst("[class*=message_text]");
            if (textNode == null) textNode = message;

            String full = textWithContacts(textNode, message, channel);
            if (full.length() < 10) continue;
            List<String> blocks = splitTasks(full, channel);
            stats[1] += blocks.size();

            for (String block : blocks) {
                String text = block.replaceAll("[\\t ]+", " ").trim();
                Lead.Category category = classify(text, store);
                if (category == null) continue;
                stats[2]++;

                String user = chooseContact(text, channel);
                if (user == null) { stats[3]++; continue; }

                String key = category.name() + "|" + user.toLowerCase(Locale.ROOT)
                        + "|" + Integer.toHexString(normalize(text).hashCode());
                out.add(new Lead(category, channel, postUrl, text, user, extractBudget(text), key));
            }
        }
        return ps;
    }

    private String textWithContacts(Element textNode, Element message, String channel) {
        Element copy = textNode.clone();
        for (Element a : copy.select("a[href]")) {
            String u = usernameFromHref(a.attr("href"));
            if (!allowed(u, channel)) continue;
            String visible = a.text() == null ? "" : a.text();
            if (!visible.toLowerCase(Locale.ROOT).contains(u.toLowerCase(Locale.ROOT))) a.after(" @" + u);
        }

        String text = copy.wholeText().trim();
        if (text.isEmpty()) text = copy.text().trim();
        text = exposeRawTelegramUrls(text, channel);

        String whole = message.wholeText() == null ? "" : message.wholeText().trim();
        whole = exposeRawTelegramUrls(whole, channel);
        Set<String> textUsers0 = collectHandles(text, channel);
        Set<String> wholeUsers = collectHandles(whole, channel);
        if (textUsers0.isEmpty() && !wholeUsers.isEmpty() && whole.length() >= text.length()) {
            text = whole;
        }

        Set<String> allUsers = collectUsersFromMessage(message, channel);
        Set<String> textUsers = collectHandles(text, channel);
        allUsers.removeAll(textUsers);

        if (allUsers.size() == 1) {
            text += "\nКонтакт для отклика: @" + allUsers.iterator().next();
        } else if (!allUsers.isEmpty()) {
            for (Element a : message.select("a[href]")) {
                String u = usernameFromHref(a.attr("href"));
                if (!allUsers.contains(u)) continue;
                String context = ((a.text() == null ? "" : a.text()) + " "
                        + (a.parent() == null ? "" : a.parent().text())).toLowerCase(Locale.ROOT);
                if (scoreContext(context) >= 2) text += "\nКонтакт для отклика: @" + u;
            }
        }
        return text;
    }

    private Set<String> collectUsersFromMessage(Element message, String channel) {
        Set<String> users = new HashSet<>();
        String messageText = message.wholeText();
        users.addAll(collectHandles(messageText, channel));
        for (Element a : message.select("a[href]")) {
            String u = usernameFromHref(a.attr("href"));
            if (allowed(u, channel)) users.add(u);
        }
        return users;
    }

    private Set<String> collectHandles(String text, String channel) {
        Set<String> users = new HashSet<>();
        if (text == null) return users;
        Matcher m = HANDLE.matcher(text);
        while (m.find()) if (allowed(m.group(1), channel)) users.add(m.group(1));
        Matcher raw = TME_RAW.matcher(text);
        while (raw.find()) if (allowed(raw.group(1), channel)) users.add(raw.group(1));
        return users;
    }

    private String exposeRawTelegramUrls(String text, String channel) {
        Matcher m = TME_RAW.matcher(text);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String u = m.group(1);
            String repl = m.group(0);
            int after = m.end();
            boolean hasPostPath = after < text.length() && text.charAt(after) == '/';
            if (allowed(u, channel) && !hasPostPath) repl = repl + " @" + u;
            m.appendReplacement(sb, Matcher.quoteReplacement(repl));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private List<String> splitTasks(String text, String channel) {
        TreeSet<Integer> starts = new TreeSet<>();
        starts.add(0);
        collect(text, NUMBER_START, starts);
        collect(text, NUMBER_INLINE, starts);
        collect(text, HASH_START, starts);
        collect(text, BULLET_START, starts);
        List<String> structural = slice(text, starts);
        List<String> result = new ArrayList<>();
        for (String b : structural) result.addAll(splitByContacts(b, channel));
        return result.isEmpty() ? java.util.Collections.singletonList(text) : result;
    }

    private List<String> splitByContacts(String text, String channel) {
        Matcher m = HANDLE.matcher(text);
        List<Integer> ends = new ArrayList<>();
        while (m.find()) {
            String u = m.group(1);
            if (!allowed(u, channel)) continue;
            int from = Math.max(0, m.start() - 140), to = Math.min(text.length(), m.end() + 80);
            String around = text.substring(from, to).toLowerCase(Locale.ROOT);
            if (scoreContext(around) >= 2 || nearLineEnd(text, m.end()) || m.end() >= text.length() - 120) ends.add(m.end());
        }
        if (ends.size() < 2) return java.util.Collections.singletonList(text);
        List<String> out = new ArrayList<>();
        int start = 0;
        for (int end : ends) {
            String p = text.substring(start, end).trim();
            if (p.length() >= 10) out.add(p);
            start = end;
        }
        if (start < text.length() && !out.isEmpty()) out.set(out.size() - 1, out.get(out.size() - 1) + " " + text.substring(start).trim());
        return out.isEmpty() ? java.util.Collections.singletonList(text) : out;
    }

    private Lead.Category classify(String text, LeadStore store) {
        String s = text.toLowerCase(Locale.ROOT);
        if (countCyrillic(s) < 6 || containsAny(s, SELF_PROMO) || isEmployment(s)) return null;
        if (store.sitesEnabled() && isSite(s)) return Lead.Category.SITE;
        if (store.presentationsEnabled() && containsAny(s, PRESENTATION_BUILD)) return Lead.Category.PRESENTATION;
        return null;
    }

    private boolean isEmployment(String s) {
        if (containsAny(s, HARD_EMPLOYMENT)) return true;
        return containsAny(s, JOB_ROLE) && containsAny(s, "обязанности:", "требования:", "условия:", "опыт работы", "remote", "удаленка", "удалёнка");
    }

    private boolean isSite(String s) {
        if (!containsAny(s, SITE_BUILD)) return false;
        String x = s.replace("не на тильде", "").replace("без тильды", "").replace("не tilda", "")
                .replace("не на wordpress", "").replace("без wordpress", "").replace("не wordpress", "");
        if (containsAny(x, "tilda", "тильд", "wordpress", "вордпресс")) return false;
        if (containsAny(s, "интернет-магазин", "интернет магазин", "маркетплейс", "личный кабинет", "backend", "saas", "crm")) return false;
        if (containsAny(s, "дизайн-макет сайта", "макет сайта в figma", "дизайн лендинга в figma", "прототип сайта")) return false;
        return true;
    }

    private String chooseContact(String text, String channel) {
        List<String> users = new ArrayList<>(collectHandles(text, channel));
        if (users.isEmpty()) return null;
        if (users.size() == 1) return users.get(0);

        Matcher m = HANDLE.matcher(text);
        String best = null;
        int bestScore = Integer.MIN_VALUE;
        while (m.find()) {
            String u = m.group(1);
            if (!allowed(u, channel)) continue;
            int from = Math.max(0, m.start() - 170), to = Math.min(text.length(), m.end() + 100);
            String around = text.substring(from, to).toLowerCase(Locale.ROOT);
            int score = scoreContext(around) + (nearLineEnd(text, m.end()) ? 3 : 0) + (m.end() >= text.length() - 170 ? 3 : 0);
            if (around.contains("канал") || around.contains("подпис")) score -= 6;
            if (score > bestScore) { bestScore = score; best = u; }
        }
        return bestScore >= 2 ? best : null;
    }

    private int scoreContext(String s) {
        int n = 0;
        for (String w : RESPONSE_WORDS) if (s.contains(w)) n += 3;
        if (s.contains("✍") || s.contains("📩") || s.contains("👉") || s.contains("📨") || s.contains("📝")) n += 5;
        return n;
    }

    private boolean nearLineEnd(String text, int pos) {
        int nl = text.indexOf('\n', pos);
        if (nl < 0) nl = text.length();
        return nl - pos <= 24;
    }

    private boolean allowed(String u, String channel) {
        if (u == null || !USERNAME.matcher(u).matches() || u.equalsIgnoreCase(channel)) return false;
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
        if (path.startsWith("s/") || path.startsWith("c/") || path.startsWith("+") || path.startsWith("joinchat/") || path.contains("/")) return null;
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
        catch (Exception e) { try { return Instant.parse(raw); } catch (Exception x) { return null; } }
    }

    private long parsePostId(String data) {
        int s = data.lastIndexOf('/');
        if (s < 0) return -1;
        try { return Long.parseLong(data.substring(s + 1)); }
        catch (Exception e) { return -1; }
    }

    private String normalize(String text) {
        return text.toLowerCase(Locale.ROOT)
                .replaceAll("https?://\\S+", " ")
                .replaceAll("@[A-Za-z0-9_]{5,32}", " ")
                .replaceAll("\\s+", " ").trim();
    }

    private void collect(String text, Pattern p, Set<Integer> s) {
        Matcher m = p.matcher(text);
        while (m.find()) s.add(m.start());
    }

    private List<String> slice(String text, TreeSet<Integer> starts) {
        List<Integer> a = new ArrayList<>(starts);
        List<String> out = new ArrayList<>();
        for (int i = 0; i < a.size(); i++) {
            int from = a.get(i), to = i + 1 < a.size() ? a.get(i + 1) : text.length();
            if (to > from) {
                String part = text.substring(from, to).trim();
                if (part.length() >= 10) out.add(part);
            }
        }
        return out.isEmpty() ? java.util.Collections.singletonList(text) : out;
    }

    private boolean containsAny(String s, String... n) {
        for (String x : n) if (x != null && !x.isEmpty() && s.contains(x)) return true;
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
        int messages, dated, fresh;
        long minPostId = Long.MAX_VALUE;
    }
}
