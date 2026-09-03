package com.alina.leadradar;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

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

/** v10 fixed scanner: Russian stem-aware, Telegram-only direct contacts. */
public final class LeadScannerV10Fixed {
    private static final Pattern HANDLE = Pattern.compile("@([A-Za-z0-9_]{5,32})");
    private static final Pattern USERNAME = Pattern.compile("^[A-Za-z0-9_]{5,32}$");
    private static final Pattern BUDGET = Pattern.compile("(?iu)(?:бюджет|оплата|цена|стоимость)?[^\\d]{0,20}(\\d{1,3}(?:[ \\u00A0]\\d{3})+|\\d{3,7})\\s*(₽|руб(?:лей|ля|\\.)?|р\\b)");
    private static final Pattern NUMBER_START = Pattern.compile("(?m)(?=^\\s*\\d{1,3}[.)]\\s+\\S)");
    private static final Pattern HASH_START = Pattern.compile("(?ium)(?=^\\s*#(?:дизайнер|презентац|сайт|лендинг|веб|web|заказ|задач)[^\\n]*)");
    private static final Pattern BULLET_START = Pattern.compile("(?ium)(?=^\\s*[•▪◾●✅☑🔹🔸⭐👉✍📌📝]\\s*(?:#|ищ|нуж|треб|задач|сдел|созд|оформ|разраб|дизайн))");

    private static final String[] EMPLOYMENT = {
            "#вакансия", "в штат", "штатный", "оформление по тк", "трудоустройство", "испытательный срок",
            "full-time", "full time", "фултайм", "part-time", "part time", "полная занятость", "частичная занятость",
            "график 5/2", "5/2", "оклад", "зарплата", "salary", "грейд", "релокация", "собеседование",
            "присылайте резюме", "отправляйте резюме", "резюме кандидата", "ставка в месяц", "часов в неделю",
            "рабочий день", "офисный формат", "на постоянной основе"
    };
    private static final String[] SELF_PROMO = {
            "ищу работу", "ищу клиентов", "оказываю услуги", "мои услуги", "портфолио моих работ",
            "заказать у меня", "предлагаю услуги", "делаю сайты", "создаю сайты", "разрабатываю сайты"
    };
    private static final String[] COMPLEX_SITE = {
            "маркетплейс", "интернет-магазин", "интернет магазин", "мобильное приложение", "backend", "бекенд",
            "личный кабинет", "fullstack", "фулстек", "saas", "crm-система", "crm система", "e-commerce", "ecommerce"
    };
    private static final String[] DESIGN_ONLY = {
            "дизайн-макет сайта", "дизайн макет сайта", "дизайн-макет лендинга", "дизайн макет лендинга",
            "макет сайта в figma", "макет лендинга в figma", "дизайн сайта в figma", "дизайн лендинга в figma",
            "только дизайн сайта", "только дизайн лендинга", "прототип сайта", "прототип лендинга"
    };
    private static final String[] INCIDENTAL_PRESENTATION = {
            "роликами с презентациями", "видео с презентациями", "формировать сметы и презентации",
            "опыт работы с презентациями", "навыки презентации", "инфографика, презентации",
            "баннеры, презентации", "посты и презентации"
    };

    public List<Lead> scanChannel(String rawChannel, LeadStore store) throws Exception {
        String channel = normalizeChannel(rawChannel);
        List<Lead> out = new ArrayList<>();
        if (channel.isEmpty()) return out;
        Instant cutoff = Instant.now().minus(Duration.ofDays(store.lookbackDays()));
        String before = null;
        Set<String> seenPosts = new HashSet<>();
        Set<Long> seenAnchors = new HashSet<>();

        while (store.running()) {
            String url = "https://t.me/s/" + channel + (before == null ? "" : "?before=" + before);
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 Chrome/131 Mobile Safari/537.36")
                    .timeout(20000).get();
            Elements messages = doc.select("div.tgme_widget_message[data-post]");
            if (messages.isEmpty()) break;
            long minPostId = Long.MAX_VALUE;
            boolean pageHasTimestamp = false;
            boolean reachedCutoff = false;

            for (Element message : messages) {
                if (!store.running()) break;
                String dataPost = message.attr("data-post");
                if (dataPost == null || dataPost.isEmpty()) continue;
                long postId = parsePostId(dataPost);
                if (postId > 0 && postId < minPostId) minPostId = postId;
                Instant postedAt = parsePostedAt(message);
                if (postedAt != null) {
                    pageHasTimestamp = true;
                    if (postedAt.isBefore(cutoff)) { reachedCutoff = true; continue; }
                }
                String postUrl = "https://t.me/" + dataPost;
                if (!seenPosts.add(postUrl)) continue;
                Element textNode = message.selectFirst("div.tgme_widget_message_text");
                if (textNode == null) continue;
                String fullText = textWithContacts(textNode, message, channel);
                if (fullText.length() < 15) continue;

                for (String task : splitTasks(fullText, channel)) {
                    String text = task.replaceAll("[\\t ]+", " ").trim();
                    if (text.length() < 15) continue;
                    Lead.Category category = classify(text, store);
                    if (category == null) continue;
                    String username = chooseContact(text, channel);
                    if (username == null) continue;
                    String normalized = normalizeForDedup(text);
                    String key = category.name() + "|" + username.toLowerCase(Locale.ROOT) + "|" + Integer.toHexString(normalized.hashCode());
                    out.add(new Lead(category, channel, postUrl, text, username, extractBudget(text), key));
                }
            }

            if (!store.running() || reachedCutoff) break;
            if (!pageHasTimestamp || minPostId == Long.MAX_VALUE || minPostId <= 1) break;
            if (!seenAnchors.add(minPostId)) break;
            before = String.valueOf(minPostId);
            try { Thread.sleep(120L); } catch (InterruptedException e) { Thread.currentThread().interrupt(); break; }
        }
        return out;
    }

    private Lead.Category classify(String text, LeadStore store) {
        String s = text.toLowerCase(Locale.ROOT);
        if (countCyrillic(s) < 6 || containsAny(s, SELF_PROMO) || containsAny(s, EMPLOYMENT)) return null;
        if (store.sitesEnabled() && isSiteOrder(s)) return Lead.Category.SITE;
        if (store.presentationsEnabled() && isPresentationOrder(s)) return Lead.Category.PRESENTATION;
        return null;
    }

    private boolean isSiteOrder(String s) {
        if (!containsAny(s, "сайт", "лендинг", "landing", "одностранич")) return false;
        if (containsAny(s, COMPLEX_SITE) || containsAny(s, DESIGN_ONLY) || requiresForbiddenPlatform(s)) return false;
        if (containsAny(s, "менеджер по продажам", "продаём готовые лендинги", "продаем готовые лендинги")) return false;
        boolean action = containsAny(s, "созда", "сдела", "разработ", "собра", "сверст", "верстк", "изготов", "под ключ");
        boolean direct = containsAny(s, "ищ", "нуж", "треб") && containsAny(s, "дизайнер", "разработчик", "верстальщик", "специалист");
        return action || direct;
    }

    private boolean isPresentationOrder(String s) {
        if (!containsAny(s, "презентац", "powerpoint", "pptx", "pitch deck", "питч-дек", "питч дек", "слайд")) return false;
        if (containsAny(s, "монтажёр", "монтажер", "видеомонтаж", "product manager", "product owner", "project manager")) return false;
        boolean action = containsAny(s, "созда", "сдела", "оформ", "собра", "подготов", "разработ", "передел", "переработ", "редизайн", "задизайн", "верст", "дизайн");
        boolean direct = containsAny(s, "нужна презентац", "нужно презентац", "заказ на презентац")
                || (containsAny(s, "ищ", "нуж", "треб") && containsAny(s, "дизайнер", "специалист", "человек"));
        if (containsAny(s, INCIDENTAL_PRESENTATION) && !action && !direct) return false;
        return action || direct;
    }

    private boolean requiresForbiddenPlatform(String s) {
        String x = s.replace("не на тильде", "").replace("не тильда", "").replace("без тильды", "")
                .replace("без tilda", "").replace("не tilda", "")
                .replace("не на wordpress", "").replace("не wordpress", "").replace("без wordpress", "")
                .replace("не на вордпресс", "").replace("не вордпресс", "").replace("без вордпресс", "");
        return containsAny(x, "tilda", "тильд", "wordpress", "вордпресс", "word press");
    }

    private List<String> splitTasks(String text, String channel) {
        TreeSet<Integer> starts = new TreeSet<>();
        collectStarts(text, NUMBER_START, starts);
        collectStarts(text, HASH_START, starts);
        collectStarts(text, BULLET_START, starts);
        List<String> blocks = new ArrayList<>();
        if (!starts.isEmpty()) {
            List<Integer> idx = new ArrayList<>(starts);
            for (int i = 0; i < idx.size(); i++) {
                int from = idx.get(i);
                int to = i + 1 < idx.size() ? idx.get(i + 1) : text.length();
                String p = text.substring(from, to).trim();
                if (p.length() >= 15) blocks.add(p);
            }
        } else blocks.add(text);

        List<String> out = new ArrayList<>();
        for (String block : blocks) {
            Matcher m = HANDLE.matcher(block);
            List<Integer> ends = new ArrayList<>();
            while (m.find()) if (isAllowedUsername(m.group(1), channel)) ends.add(m.end());
            if (ends.size() <= 1) { out.add(block); continue; }
            int start = 0;
            for (Integer end : ends) {
                String p = block.substring(start, end).trim();
                if (p.length() >= 15) out.add(p);
                start = end;
            }
            if (start < block.length() && !out.isEmpty()) {
                String tail = block.substring(start).trim();
                if (tail.length() >= 15) out.set(out.size()-1, out.get(out.size()-1) + " " + tail);
            }
        }
        return out;
    }

    private void collectStarts(String text, Pattern p, Set<Integer> starts) { Matcher m = p.matcher(text); while (m.find()) starts.add(m.start()); }

    private String textWithContacts(Element textNode, Element message, String channel) {
        Element copy = textNode.clone();
        for (Element a : copy.select("a[href]")) {
            String user = usernameFromTelegramHref(a.attr("href"));
            if (!isAllowedUsername(user, channel)) continue;
            String visible = a.text() == null ? "" : a.text();
            if (!visible.toLowerCase(Locale.ROOT).contains(user.toLowerCase(Locale.ROOT))) a.after(" @" + user);
        }
        String text = copy.wholeText().trim();
        if (text.isEmpty()) text = copy.text().trim();
        if (!HANDLE.matcher(text).find()) {
            Set<String> users = new HashSet<>();
            for (Element a : message.select("a[href]")) {
                String u = usernameFromTelegramHref(a.attr("href"));
                if (isAllowedUsername(u, channel)) users.add(u);
            }
            if (users.size() == 1) text += "\nКонтакт: @" + users.iterator().next();
        }
        return text;
    }

    private String chooseContact(String text, String channel) {
        Matcher m = HANDLE.matcher(text); String last = null;
        while (m.find()) { String u = m.group(1); if (isAllowedUsername(u, channel)) last = u; }
        return last;
    }

    private boolean isAllowedUsername(String user, String channel) {
        if (user == null || !USERNAME.matcher(user).matches()) return false;
        if (channel != null && !channel.isEmpty() && user.equalsIgnoreCase(channel)) return false;
        String l = user.toLowerCase(Locale.ROOT);
        if (l.endsWith("bot")) return false;
        return !containsAny(l, "kwork", "youdo");
    }

    private String usernameFromTelegramHref(String href) {
        if (href == null) return null;
        String h = href.trim(); String low = h.toLowerCase(Locale.ROOT);
        if (low.startsWith("tg://resolve?")) {
            int p = low.indexOf("domain="); if (p < 0) return null;
            String u = h.substring(p + 7); int amp = u.indexOf('&'); if (amp >= 0) u = u.substring(0, amp); return cleanUsername(u);
        }
        int pos = low.indexOf("t.me/"); int len = 5;
        if (pos < 0) { pos = low.indexOf("telegram.me/"); len = 12; }
        if (pos < 0) return null;
        String path = h.substring(pos + len);
        int q = path.indexOf('?'); if (q >= 0) path = path.substring(0, q);
        int hash = path.indexOf('#'); if (hash >= 0) path = path.substring(0, hash);
        while (path.startsWith("/")) path = path.substring(1);
        if (path.startsWith("s/") || path.startsWith("c/") || path.startsWith("+") || path.startsWith("joinchat/") || path.startsWith("share/") || path.contains("/")) return null;
        return cleanUsername(path);
    }

    private String cleanUsername(String raw) { if (raw == null) return null; String u = raw.trim(); while (u.startsWith("@")) u = u.substring(1); return USERNAME.matcher(u).matches() ? u : null; }
    private String extractBudget(String text) { Matcher m = BUDGET.matcher(text); if (!m.find()) return ""; return (m.group(1) + " " + m.group(2)).replace('\u00A0', ' ').trim(); }
    private Instant parsePostedAt(Element message) { Element t = message.selectFirst("time[datetime]"); if (t == null) return null; String raw = t.attr("datetime"); try { return OffsetDateTime.parse(raw).toInstant(); } catch (Exception e) { try { return Instant.parse(raw); } catch (Exception ignored) { return null; } } }
    private long parsePostId(String dataPost) { int slash = dataPost.lastIndexOf('/'); if (slash < 0) return -1; try { return Long.parseLong(dataPost.substring(slash + 1)); } catch (Exception e) { return -1; } }
    private String normalizeForDedup(String text) { return text.toLowerCase(Locale.ROOT).replaceAll("https?://\\S+", " ").replaceAll("@[A-Za-z0-9_]{5,32}", " ").replaceAll("\\s+", " ").trim(); }

    public static String normalizeChannel(String raw) {
        if (raw == null) return "";
        String s = raw.trim().replace("https://t.me/s/", "").replace("http://t.me/s/", "").replace("https://t.me/", "").replace("http://t.me/", "");
        while (s.startsWith("@")) s = s.substring(1);
        int slash = s.indexOf('/'); if (slash >= 0) s = s.substring(0, slash);
        int q = s.indexOf('?'); if (q >= 0) s = s.substring(0, q);
        return USERNAME.matcher(s).matches() ? s : "";
    }

    private boolean containsAny(String s, String... needles) { for (String n : needles) if (n != null && !n.isEmpty() && s.contains(n)) return true; return false; }
    private int countCyrillic(String s) { int n=0; for(int i=0;i<s.length();i++){char c=s.charAt(i); if((c>='А'&&c<='я')||c=='ё'||c=='Ё') n++;} return n; }
}
