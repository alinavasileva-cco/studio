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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * v7 scanner: only concrete quick/project orders for presentations and simple sites.
 * A mere mention of "presentation" or "landing" inside a vacancy is NOT enough.
 */
public final class LeadScannerV7 {
    private static final Pattern HANDLE = Pattern.compile("@([A-Za-z0-9_]{5,32})");
    private static final Pattern BUDGET = Pattern.compile("(?iu)(?:бюджет|оплата|цена)?[^\\d]{0,20}(\\d{1,3}(?:[ \\u00A0]\\d{3})+|\\d{3,7})\\s*(₽|руб(?:лей|ля|\\.)?|р\\b)");
    private static final Pattern NUMBERED_TASK = Pattern.compile("(?iu)(?=(?:[1-9]|1\\d|20)[\\.)]\\s*#)");
    private static final Pattern BULLET_TASK = Pattern.compile("(?iu)(?=[•▪◾●]\\s*#)");

    private static final String[] SELF_PROMO = {
            "ищу работу", "ищу клиентов", "оказываю услуги", "мои услуги", "создам вам", "делаю сайты",
            "могу сделать сайт", "разрабатываю сайты", "создаю сайты", "сделаю лендинг", "создам лендинг",
            "портфолио моих работ", "заказать у меня", "мой телеграмм для заказа", "пишите мне за услуг"
    };

    private static final String[] HARD_EMPLOYMENT = {
            "#вакансия", "в штат", "штатный", "оформление по тк", "трудоустройство", "испытательный срок",
            "full-time", "full time", "фултайм", "part-time", "part time", "полная занятость",
            "частичная занятость", "график 5/2", "5/2", "оклад", "зарплата", "salary", "грейд",
            "релокация", "собеседование", "резюме кандидата", "присылайте резюме", "отправляйте резюме"
    };

    private static final String[] JOB_ROLE_NOISE = {
            "product manager", "product owner", "project manager", "проджект менеджер", "проектный менеджер",
            "менеджер проектов", "менеджер по продажам", "sales manager", "продажник", "account manager",
            "аккаунт-менеджер", "ui/ux designer", "ux/ui designer", "middle designer", "senior designer",
            "middle+", "senior ", "outstaff", "аутстафф"
    };

    private static final String[] JOB_STRUCTURE = {
            "обязанности:", "требования:", "условия:", "что важно в опыте", "кого ищем:",
            "формат: full", "формат работы: full", "опыт работы от", "опыт от 2", "опыт от 3",
            "на постоянной основе", "долгосрочное сотрудничество", "ежемесячно", "в месяц"
    };

    private static final String[] COMPLEX_SITE = {
            "маркетплейс", "интернет-магазин", "интернет магазин", "мобильное приложение", "backend", "бекенд",
            "личный кабинет", "fullstack", "фулстек", "saas", "crm-система", "crm система", "e-commerce", "ecommerce"
    };

    private static final String[] SITE_BUILD = {
            "сделать сайт", "создать сайт", "разработать сайт", "собрать сайт", "сверстать сайт",
            "создание сайта", "разработка сайта", "сайт с нуля", "сайт под ключ", "нужен сайт", "нужно сделать сайт",
            "сделать лендинг", "создать лендинг", "разработать лендинг", "собрать лендинг", "сверстать лендинг",
            "создание лендинга", "разработка лендинга", "лендинг с нуля", "лендинг под ключ", "нужен лендинг",
            "нужно сделать лендинг", "одностраничный сайт под ключ", "сайт-визитку", "сайт визитку"
    };

    private static final String[] SITE_DESIGN_ONLY = {
            "дизайн-макет сайта", "дизайн макет сайта", "дизайн-макет лендинга", "дизайн макет лендинга",
            "макет сайта в figma", "макет лендинга в figma", "дизайн сайта в figma", "дизайн лендинга в figma",
            "только дизайн сайта", "только дизайн лендинга", "прототип сайта", "прототип лендинга"
    };

    private static final String[] PRESENTATION_BUILD = {
            "сделать презентац", "создать презентац", "оформить презентац", "собрать презентац", "подготовить презентац",
            "разработать презентац", "переделать презентац", "переработать презентац", "редизайн презентац",
            "задизайнить презентац", "дизайн презентац", "верстка презентац", "вёрстка презентац",
            "оформление презентац", "пересобрать презентац", "собрать слайды", "оформить слайды"
    };

    private static final String[] PRESENTATION_DIRECT_REQUEST = {
            "ищу дизайнера презентац", "ищем дизайнера презентац", "нужен дизайнер презентац",
            "нужна презентация", "нужно сделать презентац", "требуется дизайнер презентац",
            "разовая задача по презентац", "разовая задача: презентац", "заказ на презентац"
    };

    private static final String[] INCIDENTAL_PRESENTATION = {
            "роликами с презентациями", "видео с презентациями", "подкастами и влогами", "формировать сметы и презентации",
            "готовить коммерческие предложения", "опыт работы с презентациями", "навыки презентации",
            "инфографика, презентации", "баннеры, презентации", "посты и презентации"
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

                Element textNode = message.selectFirst("div.tgme_widget_message_text");
                if (textNode == null) continue;
                String fullText = textNode.wholeText().trim();
                if (fullText.isEmpty()) fullText = textNode.text().trim();
                if (fullText.length() < 15) continue;

                List<String> tasks = splitTasks(fullText, channel);
                for (String task : tasks) {
                    if (!store.running()) break;
                    String text = task.replaceAll("[\\t ]+", " ").trim();
                    if (text.length() < 15) continue;

                    Lead.Category category = classify(text, store);
                    if (category == null) continue;

                    String contactRef = chooseDirectContact(text, channel);
                    if (contactRef == null || contactRef.isEmpty()) {
                        contactRef = chooseExternalTaskLink(message);
                    }
                    if (contactRef == null || contactRef.isEmpty()) continue;

                    String normalizedTask = normalizeForDedup(text);
                    String dedupKey = category.name() + "|" + contactRef.toLowerCase(Locale.ROOT)
                            + "|" + Integer.toHexString(normalizedTask.hashCode());
                    out.add(new Lead(category, channel, postUrl, text, contactRef, extractBudget(text), dedupKey));
                }
            }

            if (!store.running() || reachedCutoff) break;
            if (!pageHasTimestamp || minPostId == Long.MAX_VALUE || minPostId <= 1) break;
            if (!seenAnchors.add(minPostId)) break;
            before = String.valueOf(minPostId);
            try { Thread.sleep(140L); } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        return out;
    }

    private Lead.Category classify(String text, LeadStore store) {
        String s = text.toLowerCase(Locale.ROOT);
        if (countCyrillic(s) < 8) return null;
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
        if (role && structure) return true;
        if (role && containsAny(s, "удаленка", "удалёнка", "remote") && containsAny(s, "опыт", "команд", "проект")) return true;
        return false;
    }

    private boolean isSiteOrder(String s) {
        if (!containsAny(s, "сайт", "лендинг", "landing", "одностранич")) return false;
        if (containsAny(s, COMPLEX_SITE)) return false;
        if (requiresForbiddenPlatform(s)) return false;
        if (requiresBrandWork(s)) return false;
        if (containsAny(s, "продаём готовые лендинги", "продаем готовые лендинги", "продаём лендинги", "продаем лендинги",
                "менеджер по продажам", "ваша задача: найти владельца бизнеса", "ваша задача — найти владельца бизнеса")) return false;
        if (containsAny(s, SITE_DESIGN_ONLY)) return false;
        return containsAny(s, SITE_BUILD);
    }

    private boolean isPresentationOrder(String s) {
        if (!containsAny(s, "презентац", "pitch deck", "питч-дек", "питч дек", "powerpoint", "pptx", "слайды")) return false;
        if (requiresBrandWork(s)) return false;
        if (containsAny(s, INCIDENTAL_PRESENTATION) && !containsAny(s, PRESENTATION_BUILD) && !containsAny(s, PRESENTATION_DIRECT_REQUEST)) return false;
        if (containsAny(s, "монтажёр", "монтажер", "видеомонтаж", "product manager", "product owner", "project manager")
                && !containsAny(s, PRESENTATION_BUILD)) return false;
        return containsAny(s, PRESENTATION_BUILD) || containsAny(s, PRESENTATION_DIRECT_REQUEST);
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

    private List<String> splitTasks(String text, String channel) {
        List<String> numbered = splitByPattern(text, NUMBERED_TASK);
        if (numbered.size() > 1) return numbered;
        List<String> bullets = splitByPattern(text, BULLET_TASK);
        if (bullets.size() > 1) return bullets;

        List<Integer> contactEnds = new ArrayList<>();
        Matcher m = HANDLE.matcher(text);
        while (m.find()) {
            String u = m.group(1);
            if (u.equalsIgnoreCase(channel) || u.toLowerCase(Locale.ROOT).endsWith("bot")) continue;
            contactEnds.add(m.end());
        }
        if (contactEnds.size() < 2) {
            List<String> one = new ArrayList<>(); one.add(text); return one;
        }
        List<String> parts = new ArrayList<>();
        int start = 0;
        for (Integer end : contactEnds) {
            if (end <= start) continue;
            String part = text.substring(start, end).trim();
            if (part.length() >= 15) parts.add(part);
            start = end;
        }
        return parts.isEmpty() ? java.util.Collections.singletonList(text) : parts;
    }

    private List<String> splitByPattern(String text, Pattern pattern) {
        String[] raw = pattern.split(text);
        List<String> out = new ArrayList<>();
        for (String part : raw) if (part.trim().length() >= 15) out.add(part.trim());
        return out;
    }

    private String chooseDirectContact(String text, String channel) {
        Matcher m = HANDLE.matcher(text);
        String best = null;
        int bestScore = Integer.MIN_VALUE;
        String lower = text.toLowerCase(Locale.ROOT);
        while (m.find()) {
            String user = m.group(1);
            String u = user.toLowerCase(Locale.ROOT);
            if (user.equalsIgnoreCase(channel) || u.endsWith("bot")) continue;
            int from = Math.max(0, m.start() - 90);
            int to = Math.min(text.length(), m.end() + 30);
            String around = lower.substring(from, to);
            int score = 0;
            if (containsAny(around, "контакт", "пишите", "писать", "напишите", "отклик", "лс", "личк")) score += 10;
            if (containsAny(around, "реклама", "админ", "менеджер канала")) score -= 8;
            if (score >= bestScore) { best = user; bestScore = score; }
        }
        return best;
    }

    private String chooseExternalTaskLink(Element message) {
        for (Element a : message.select("a[href]")) {
            String href = a.attr("href");
            if (href == null) continue;
            String h = href.toLowerCase(Locale.ROOT);
            if ((h.contains("youdo.com/t") || h.contains("kwork.ru/projects") || h.contains("kwork.ru/project")
                    || h.contains("freelance.ru/project") || h.contains("fl.ru/projects")) && href.startsWith("http")) {
                return "url:" + href;
            }
        }
        return null;
    }

    private String extractBudget(String text) {
        Matcher m = BUDGET.matcher(text);
        if (!m.find()) return "";
        return (m.group(1) + " " + m.group(2)).replace('\u00A0', ' ').trim();
    }

    private Instant parsePostedAt(Element message) {
        Element time = message.selectFirst("time[datetime]");
        if (time == null) return null;
        try { return OffsetDateTime.parse(time.attr("datetime")).toInstant(); }
        catch (Exception ignored) { return null; }
    }

    private long parsePostId(String dataPost) {
        int slash = dataPost.lastIndexOf('/');
        if (slash < 0 || slash + 1 >= dataPost.length()) return -1;
        try { return Long.parseLong(dataPost.substring(slash + 1)); }
        catch (Exception ignored) { return -1; }
    }

    private String normalizeForDedup(String s) {
        return s.toLowerCase(Locale.ROOT).replaceAll("https?://\\S+", " ").replaceAll("@\\w+", " ")
                .replaceAll("[^a-zа-яё0-9]+", " ").replaceAll("\\s+", " ").trim();
    }

    private int countCyrillic(String s) {
        int n = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if ((c >= 'А' && c <= 'я') || c == 'Ё' || c == 'ё') n++;
        }
        return n;
    }

    private boolean containsAny(String s, String... values) {
        for (String v : values) if (s.contains(v)) return true;
        return false;
    }

    public static String normalizeChannel(String raw) {
        if (raw == null) return "";
        String c = raw.trim();
        c = c.replace("https://t.me/s/", "").replace("http://t.me/s/", "")
                .replace("https://t.me/", "").replace("http://t.me/", "");
        if (c.startsWith("@")) c = c.substring(1);
        int q = c.indexOf('?'); if (q >= 0) c = c.substring(0, q);
        int slash = c.indexOf('/'); if (slash >= 0) c = c.substring(0, slash);
        return c.replaceAll("[^A-Za-z0-9_]", "");
    }
}
