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

/**
 * v10: flexible quick-order scanner.
 * Reads the public Telegram history, splits digest posts into independent tasks,
 * understands non-adjacent wording like "кто сделает быстро красивую презентацию",
 * and only keeps tasks with a direct Telegram contact.
 */
public final class LeadScannerV10 {
    private static final Pattern HANDLE = Pattern.compile("@([A-Za-z0-9_]{5,32})");
    private static final Pattern USERNAME = Pattern.compile("^[A-Za-z0-9_]{5,32}$");
    private static final Pattern BUDGET = Pattern.compile("(?iu)(?:бюджет|оплата|цена|ставка)?[^\\d]{0,20}(\\d{1,3}(?:[ \\u00A0]\\d{3})+|\\d{3,7})\\s*(₽|руб(?:лей|ля|\\.)?|р\\b)");

    private static final Pattern ACTION = Pattern.compile("(?iu)(ищу|ищем|ищет|нужен|нужна|нужно|требуется|требуются|кто\\s+может|кто\\s+сделает|заказ|задача|сделать|сделает|делать|создать|создание|оформить|оформление|собрать|сборка|разработать|разработка|переделать|переработать|редизайн|задизайнить|дизайн|верстка|вёрстка)");
    private static final Pattern PRESENTATION_TARGET = Pattern.compile("(?iu)(презентац\\w*|слайд\\w*|powerpoint|pptx|pitch\\s*deck|питч[- ]?дек)");
    private static final Pattern SITE_TARGET = Pattern.compile("(?iu)(лендинг\\w*|landing\\w*|сайт[- ]?визитк\\w*|одностраничн\\w*\\s+сайт\\w*|прост\\w*\\s+сайт\\w*|\\bсайт\\w*)");

    private static final Pattern NUMBER_START = Pattern.compile("(?m)(?=^\\s*\\d{1,3}[.)]\\s+\\S)");
    private static final Pattern HASH_START = Pattern.compile("(?ium)(?=^\\s*#(?:ищу|заказ|задач|дизайнер|презентац|сайт|лендинг|веб|web)[^\\n]*)");
    private static final Pattern BULLET_START = Pattern.compile("(?ium)(?=^\\s*[•▪◾●✅☑🔹🔸⭐👉✍📌]\\s*(?:#|ищ|нуж|треб|задач|сдел|созд|оформ|разраб|дизайн))");

    private static final String[] SELF_PROMO = {
            "ищу работу", "ищу клиентов", "оказываю услуги", "мои услуги", "создам вам", "делаю сайты",
            "могу сделать сайт", "разрабатываю сайты", "создаю сайты", "сделаю лендинг", "создам лендинг",
            "портфолио моих работ", "заказать у меня", "мой телеграмм для заказа", "пишите мне за услуг"
    };

    private static final String[] HARD_EMPLOYMENT = {
            "#вакансия", "в штат", "штатный", "оформление по тк", "трудоустройство", "испытательный срок",
            "full-time", "full time", "фултайм", "полная занятость", "график 5/2", "5/2", "оклад", "зарплата",
            "salary", "грейд", "релокация", "собеседование", "резюме кандидата", "присылайте резюме",
            "отправляйте резюме", "ставка в месяц", "рабочий день", "офисный формат"
    };

    private static final String[] JOB_ROLE_NOISE = {
            "product manager", "product owner", "project manager", "проджект менеджер", "проектный менеджер",
            "менеджер проектов", "менеджер по продажам", "sales manager", "account manager", "аккаунт-менеджер",
            "ui/ux designer", "ux/ui designer", "middle designer", "senior designer", "middle+", "senior ",
            "outstaff", "аутстафф"
    };

    private static final String[] JOB_STRUCTURE = {
            "обязанности:", "требования:", "условия:", "что важно в опыте", "кого ищем:",
            "формат: full", "формат работы: full", "опыт работы от", "на постоянной основе", "ежемесячно"
    };

    private static final String[] COMPLEX_SITE = {
            "маркетплейс", "интернет-магазин", "интернет магазин", "мобильное приложение", "backend", "бекенд",
            "личный кабинет", "fullstack", "фулстек", "saas", "crm-система", "crm система", "e-commerce", "ecommerce"
    };

    private static final String[] SITE_DESIGN_ONLY = {
            "дизайн-макет сайта", "дизайн макет сайта", "дизайн-макет лендинга", "дизайн макет лендинга",
            "макет сайта в figma", "макет лендинга в figma", "дизайн сайта в figma", "дизайн лендинга в figma",
            "только дизайн сайта", "только дизайн лендинга", "прототип сайта", "прототип лендинга"
    };

    private static final String[] MEDIA_NOISE = {
            "монтажёр", "монтажер", "видеомонтаж", "youtube", "ютуб", "ролик", "reels", "рилс", "подкаст"
    };

    private static final String[] RESPONSE_WORDS = {
            "пишите", "писать", "напишите", "отклик", "контакт", "связь", "связаться", "телеграм", "telegram",
            "тг", "личку", "лс", "присылайте", "портфолио", "обращаться", "обращайтесь"
    };

    public List<Lead> scanChannel(String rawChannel, LeadStore store) throws Exception {
        String channel = normalizeChannel(rawChannel);
        List<Lead> out = new ArrayList<>();
        if (channel.isEmpty()) return out;

        int postsRead = 0;
        int taskBlocks = 0;
        int categoryCandidates = 0;
        int noContact = 0;
        Instant cutoff = Instant.now().minus(Duration.ofDays(store.lookbackDays()));
        String before = null;
        Set<String> seenPosts = new HashSet<>();
        Set<Long> seenAnchors = new HashSet<>();

        try {
            while (store.running()) {
                String url = "https://t.me/s/" + channel + (before == null ? "" : "?before=" + before);
                Document doc = Jsoup.connect(url)
                        .userAgent("Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 Chrome/131 Mobile Safari/537.36")
                        .timeout(20000)
                        .get();

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
                        if (postedAt.isBefore(cutoff)) {
                            reachedCutoff = true;
                            continue;
                        }
                    }

                    String postUrl = "https://t.me/" + dataPost;
                    if (!seenPosts.add(postUrl)) continue;
                    postsRead++;

                    Element textNode = message.selectFirst("div.tgme_widget_message_text");
                    if (textNode == null) continue;
                    String fullText = textWithVisibleTelegramContacts(textNode, message, channel);
                    if (fullText.length() < 15) continue;

                    List<String> tasks = splitTasks(fullText, channel);
                    for (String task : tasks) {
                        if (!store.running()) break;
                        String text = task.replaceAll("[\\t ]+", " ").trim();
                        if (text.length() < 15) continue;
                        taskBlocks++;

                        Lead.Category category = classify(text, store);
                        if (category == null) continue;
                        categoryCandidates++;

                        String username = chooseDirectTelegramContact(text, channel);
                        if (username == null || username.isEmpty()) {
                            noContact++;
                            continue;
                        }

                        String normalizedTask = normalizeForDedup(text);
                        String dedupKey = category.name() + "|" + username.toLowerCase(Locale.ROOT)
                                + "|" + Integer.toHexString(normalizedTask.hashCode());
                        out.add(new Lead(category, channel, postUrl, text, username, extractBudget(text), dedupKey));
                    }
                }

                if (!store.running() || reachedCutoff) break;
                if (!pageHasTimestamp || minPostId == Long.MAX_VALUE || minPostId <= 1) break;
                if (!seenAnchors.add(minPostId)) break;
                before = String.valueOf(minPostId);
                try { Thread.sleep(120L); }
                catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        } finally {
            store.addDiagnostics(postsRead, taskBlocks, categoryCandidates, noContact);
        }
        return out;
    }

    private Lead.Category classify(String text, LeadStore store) {
        String s = text.toLowerCase(Locale.ROOT);
        if (countCyrillic(s) < 6) return null;
        if (containsAny(s, SELF_PROMO)) return null;
        if (isEmployment(s)) return null;

        if (store.sitesEnabled() && isSiteOrder(s)) return Lead.Category.SITE;
        if (store.presentationsEnabled() && isPresentationOrder(s)) return Lead.Category.PRESENTATION;
        return null;
    }

    private boolean isEmployment(String s) {
        if (containsAny(s, HARD_EMPLOYMENT)) return true;
        boolean role = containsAny(s, JOB_ROLE_NOISE);
        boolean structure = containsAny(s, JOB_STRUCTURE);
        return role && structure;
    }

    private boolean isSiteOrder(String s) {
        if (!SITE_TARGET.matcher(s).find()) return false;
        if (containsAny(s, COMPLEX_SITE)) return false;
        if (requiresForbiddenPlatform(s)) return false;
        if (requiresBrandWork(s)) return false;
        if (containsAny(s, SITE_DESIGN_ONLY)) return false;
        if (containsAny(s, "продаём готовые лендинги", "продаем готовые лендинги", "менеджер по продажам")) return false;
        return hasFlexibleOrder(s, SITE_TARGET, 240);
    }

    private boolean isPresentationOrder(String s) {
        if (!PRESENTATION_TARGET.matcher(s).find()) return false;
        if (requiresBrandWork(s)) return false;
        if (containsAny(s, MEDIA_NOISE) && !hasStrongPresentationAction(s)) return false;
        return hasFlexibleOrder(s, PRESENTATION_TARGET, 240);
    }

    private boolean hasStrongPresentationAction(String s) {
        return s.matches("(?is).*(сделать|сделает|создать|оформить|оформление|собрать|переделать|переработать|редизайн|дизайн)[^.!?\\n]{0,180}(презентац|слайд).*"
                ) || s.matches("(?is).*(презентац|слайд)[^.!?\\n]{0,180}(сделать|создать|оформить|собрать|переделать|редизайн|дизайн).*" );
    }

    private boolean hasFlexibleOrder(String s, Pattern target, int maxDistance) {
        List<Integer> actions = starts(ACTION, s);
        List<Integer> targets = starts(target, s);
        for (Integer a : actions) {
            for (Integer t : targets) {
                if (Math.abs(a - t) <= maxDistance) return true;
            }
        }
        return false;
    }

    private List<Integer> starts(Pattern p, String s) {
        List<Integer> out = new ArrayList<>();
        Matcher m = p.matcher(s);
        while (m.find()) out.add(m.start());
        return out;
    }

    private boolean requiresForbiddenPlatform(String s) {
        String x = s
                .replace("не на тильде", "").replace("не тильда", "").replace("без тильды", "")
                .replace("без tilda", "").replace("не tilda", "")
                .replace("не на wordpress", "").replace("не wordpress", "").replace("без wordpress", "")
                .replace("не на вордпресс", "").replace("не вордпресс", "").replace("без вордпресс", "");
        return containsAny(x, "tilda", "тильд", "wordpress", "вордпресс", "word press");
    }

    private boolean requiresBrandWork(String s) {
        String x = s
                .replace("логотип готов", "").replace("лого готов", "").replace("логотип уже есть", "")
                .replace("фирменный стиль готов", "").replace("фирменный стиль уже есть", "")
                .replace("есть фирменный стиль", "").replace("по фирменному стилю", "")
                .replace("в фирменном стиле", "");
        return containsAny(x, "создать логотип", "сделать логотип", "разработать логотип", "нужен логотип",
                "айдентик", "брендбук", "разработать фирменный стиль", "создать фирменный стиль", "брендинг под ключ");
    }

    private String textWithVisibleTelegramContacts(Element textNode, Element message, String channel) {
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

    private List<String> splitTasks(String text, String channel) {
        List<ContactHit> contacts = directContactHits(text, channel);
        if (contacts.size() >= 2) {
            List<String> byContact = new ArrayList<>();
            int start = 0;
            for (ContactHit hit : contacts) {
                int end = hit.end;
                if (end > start) {
                    String part = text.substring(start, end).trim();
                    if (part.length() >= 15) byContact.add(part);
                }
                start = end;
            }
            if (!byContact.isEmpty()) return byContact;
        }

        TreeSet<Integer> starts = new TreeSet<>();
        starts.add(0);
        collectStarts(text, NUMBER_START, starts);
        collectStarts(text, HASH_START, starts);
        collectStarts(text, BULLET_START, starts);
        if (starts.size() <= 1) return java.util.Collections.singletonList(text);

        List<Integer> idx = new ArrayList<>(starts);
        List<String> parts = new ArrayList<>();
        for (int i = 0; i < idx.size(); i++) {
            int from = idx.get(i);
            int to = i + 1 < idx.size() ? idx.get(i + 1) : text.length();
            String p = text.substring(from, to).trim();
            if (p.length() >= 15) parts.add(p);
        }
        return parts.isEmpty() ? java.util.Collections.singletonList(text) : parts;
    }

    private void collectStarts(String text, Pattern pattern, Set<Integer> starts) {
        Matcher m = pattern.matcher(text);
        while (m.find()) starts.add(m.start());
    }

    private List<ContactHit> directContactHits(String text, String channel) {
        List<ContactHit> out = new ArrayList<>();
        Matcher m = HANDLE.matcher(text);
        while (m.find()) {
            String user = m.group(1);
            if (isAllowedUsername(user, channel)) out.add(new ContactHit(user, m.start(), m.end()));
        }
        return out;
    }

    private String chooseDirectTelegramContact(String text, String channel) {
        List<ContactHit> hits = directContactHits(text, channel);
        if (hits.isEmpty()) return null;
        if (hits.size() == 1) return hits.get(0).user;

        String lower = text.toLowerCase(Locale.ROOT);
        String best = null;
        int bestScore = Integer.MIN_VALUE;
        for (ContactHit h : hits) {
            int from = Math.max(0, h.start - 120);
            int to = Math.min(text.length(), h.end + 80);
            String around = lower.substring(from, to);
            int score = 0;
            for (String w : RESPONSE_WORDS) if (around.contains(w)) score += 3;
            if (around.contains("контакт")) score += 5;
            if (h.end >= text.length() - 100) score += 2;
            if (around.contains("канал") || around.contains("подпис")) score -= 5;
            if (score > bestScore) { bestScore = score; best = h.user; }
        }
        return best;
    }

    private boolean isAllowedUsername(String user, String channel) {
        if (user == null || !USERNAME.matcher(user).matches()) return false;
        if (user.equalsIgnoreCase(channel)) return false;
        String l = user.toLowerCase(Locale.ROOT);
        if (l.endsWith("bot")) return false;
        return !containsAny(l, "kwork", "youdo");
    }

    private String usernameFromTelegramHref(String href) {
        if (href == null || href.trim().isEmpty()) return null;
        String h = href.trim();
        String lower = h.toLowerCase(Locale.ROOT);
        if (lower.startsWith("tg://resolve?")) {
            int p = lower.indexOf("domain=");
            if (p < 0) return null;
            String u = h.substring(p + 7);
            int amp = u.indexOf('&');
            if (amp >= 0) u = u.substring(0, amp);
            return cleanUsername(u);
        }
        int marker = lower.indexOf("t.me/");
        int markerLen = 5;
        if (marker < 0) { marker = lower.indexOf("telegram.me/"); markerLen = 12; }
        if (marker < 0) return null;
        String path = h.substring(marker + markerLen);
        int q = path.indexOf('?'); if (q >= 0) path = path.substring(0, q);
        int hash = path.indexOf('#'); if (hash >= 0) path = path.substring(0, hash);
        while (path.startsWith("/")) path = path.substring(1);
        if (path.startsWith("s/") || path.startsWith("c/") || path.startsWith("+") || path.startsWith("joinchat/")
                || path.startsWith("share/") || path.startsWith("iv/") || path.contains("/")) return null;
        return cleanUsername(path);
    }

    private String cleanUsername(String raw) {
        if (raw == null) return null;
        String u = raw.trim();
        while (u.startsWith("@")) u = u.substring(1);
        return USERNAME.matcher(u).matches() ? u : null;
    }

    private String extractBudget(String text) {
        Matcher m = BUDGET.matcher(text);
        if (!m.find()) return "";
        return (m.group(1) + " " + m.group(2)).replace('\u00A0', ' ').trim();
    }

    private Instant parsePostedAt(Element message) {
        Element t = message.selectFirst("time[datetime]");
        if (t == null) return null;
        String raw = t.attr("datetime");
        try { return OffsetDateTime.parse(raw).toInstant(); }
        catch (Exception ignored) {
            try { return Instant.parse(raw); }
            catch (Exception ignored2) { return null; }
        }
    }

    private long parsePostId(String dataPost) {
        int slash = dataPost.lastIndexOf('/');
        if (slash < 0 || slash + 1 >= dataPost.length()) return -1;
        try { return Long.parseLong(dataPost.substring(slash + 1)); }
        catch (Exception ignored) { return -1; }
    }

    private String normalizeForDedup(String text) {
        return text.toLowerCase(Locale.ROOT)
                .replaceAll("https?://\\S+", " ")
                .replaceAll("@[A-Za-z0-9_]{5,32}", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    public static String normalizeChannel(String raw) {
        if (raw == null) return "";
        String s = raw.trim();
        if (s.isEmpty()) return "";
        s = s.replace("https://t.me/s/", "").replace("http://t.me/s/", "")
                .replace("https://t.me/", "").replace("http://t.me/", "");
        while (s.startsWith("@")) s = s.substring(1);
        int slash = s.indexOf('/'); if (slash >= 0) s = s.substring(0, slash);
        int q = s.indexOf('?'); if (q >= 0) s = s.substring(0, q);
        return USERNAME.matcher(s).matches() ? s : "";
    }

    private boolean containsAny(String s, String... needles) {
        if (s == null || needles == null) return false;
        for (String n : needles) if (n != null && !n.isEmpty() && s.contains(n)) return true;
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

    private static final class ContactHit {
        final String user;
        final int start;
        final int end;
        ContactHit(String user, int start, int end) { this.user = user; this.start = start; this.end = end; }
    }
}
