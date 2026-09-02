package com.alina.leadradar;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class LeadScanner {
    private static final Pattern HANDLE = Pattern.compile("@([A-Za-z0-9_]{5,32})");
    private static final Pattern BUDGET = Pattern.compile("(?iu)(?:бюджет[^\\d]{0,20})?(\\d{1,3}(?:[ \\u00A0]\\d{3})+|\\d{4,7})\\s*(₽|руб(?:лей|ля|\\.)?|р\\b)");
    private static final Pattern NUMBERED_TASK = Pattern.compile("(?iu)(?=(?:[1-9]|1\\d|20)[\\.)]\\s*#)");
    private static final Pattern BULLET_TASK = Pattern.compile("(?iu)(?=[•▪◾●]\\s*#)");

    private static final String[] INTENT = {
            "ищу", "ищем", "ищется", "нужен", "нужна", "нужны", "нужно", "требуется", "требуются",
            "задача", "заказ", "подрядчик", "исполнитель", "кто может", "кто сможет", "пишите", "напишите",
            "отклик", "присылайте", "контакт"
    };

    private static final String[] SELF_PROMO = {
            "ищу работу", "ищу клиентов", "оказываю услуги", "мои услуги", "создам вам", "делаю сайты",
            "могу сделать сайт", "разрабатываю сайты", "создаю сайты", "сделаю лендинг", "создам лендинг",
            "возьму заказ", "готов взять", "портфолио моих работ", "обращайтесь", "заказать у меня"
    };

    private static final String[] EMPLOYMENT = {
            "#вакансия", "вакансия", "в штат", "штатный", "полная занятость", "частичная занятость",
            "оформление по тк", "трудоустройство", "собеседование", "резюме", "зарплата", "оклад",
            "график работы", "испытательный срок", "в команду", "на постоянной основе", "постоянное сотрудничество",
            "долгосрочное сотрудничество", "регулярные заказы", "ежемесячно", "еженедельно", "ставка в месяц"
    };

    private static final String[] COMPLEX_SITE = {
            "маркетплейс", "интернет-магазин", "интернет магазин", "мобильное приложение", "мобильного приложения",
            "backend", "бекенд", "личный кабинет", "fullstack", "фулстек", "saas", "crm-система", "crm система",
            "онлайн-магазин", "каталог товаров", "интернет витрина", "e-commerce", "ecommerce"
    };

    private static final String[] SITE_OBJECT = {
            "сайт", "лендинг", "landing", "сайт-визит", "одностраничный сайт", "одностраничник", "landing page"
    };

    private static final String[] SITE_BUILD_ACTION = {
            "сделать сайт", "создать сайт", "разработать сайт", "собрать сайт", "сверстать сайт", "верстка сайта",
            "сделать лендинг", "создать лендинг", "разработать лендинг", "собрать лендинг", "сверстать лендинг",
            "верстка лендинга", "создание сайта", "разработка сайта", "создание лендинга", "разработка лендинга",
            "сайт с нуля", "лендинг с нуля", "сайт под ключ", "лендинг под ключ", "нужен сайт", "нужен лендинг",
            "нужно сделать сайт", "нужно сделать лендинг", "кто сделает сайт", "кто сделает лендинг"
    };

    private static final String[] DESIGN_ONLY_SITE = {
            "только дизайн", "дизайн-макет сайта", "дизайн макет сайта", "дизайн-макет лендинга", "дизайн макет лендинга",
            "прототип сайта", "прототип лендинга", "отрисовать сайт", "отрисовать лендинг", "макет сайта в figma",
            "макет лендинга в figma", "дизайн сайта в figma", "дизайн лендинга в figma", "только макет"
    };

    private static final String[] PRESENTATION_OBJECT = {
            "презентац", "pitch deck", "питч-дек", "питч дек", "powerpoint", "pptx"
    };

    private static final String[] PRESENTATION_ACTION = {
            "сделать презентац", "создать презентац", "оформить презентац", "собрать презентац", "подготовить презентац",
            "разработать презентац", "переделать презентац", "переработать презентац", "редизайн презентац",
            "задизайнить презентац", "дизайн презентац", "верстка презентац", "оформление презентац"
    };

    public List<Lead> scanChannel(String rawChannel, LeadStore store) throws Exception {
        String channel = normalizeChannel(rawChannel);
        List<Lead> out = new ArrayList<>();
        if (channel.isEmpty()) return out;

        Document doc = Jsoup.connect("https://t.me/s/" + channel)
                .userAgent("Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 Chrome/131 Mobile Safari/537.36")
                .timeout(20000)
                .get();

        Elements messages = doc.select("div.tgme_widget_message[data-post]");
        for (Element message : messages) {
            String dataPost = message.attr("data-post");
            if (dataPost == null || dataPost.isEmpty()) continue;
            String postUrl = "https://t.me/" + dataPost;
            if (!isFresh(message)) continue;

            Element textNode = message.selectFirst("div.tgme_widget_message_text");
            if (textNode == null) continue;
            String fullText = textNode.wholeText().trim();
            if (fullText.isEmpty()) fullText = textNode.text().trim();
            if (fullText.length() < 20) continue;

            boolean inheritedIntent = containsAny(fullText.toLowerCase(Locale.ROOT), INTENT);
            List<String> tasks = splitTasks(fullText, channel);
            for (String task : tasks) {
                String text = task.replaceAll("[\\t ]+", " ").trim();
                if (text.length() < 20) continue;

                Lead.Category category = classify(text, store, inheritedIntent);
                if (category == null) continue;

                String username = chooseContact(text, channel);
                if (username == null || username.isEmpty()) continue;

                String dedupKey = postUrl + "|" + category.name() + "|" + username.toLowerCase(Locale.ROOT)
                        + "|" + Integer.toHexString(text.toLowerCase(Locale.ROOT).hashCode());
                if (store.wasSent(dedupKey)) continue;

                String budget = extractBudget(text);
                out.add(new Lead(category, channel, postUrl, text, username, budget, dedupKey));
            }
        }
        return out;
    }

    private Lead.Category classify(String text, LeadStore store, boolean inheritedIntent) {
        String s = text.toLowerCase(Locale.ROOT);
        if (countCyrillic(s) < 12) return null;
        if (containsAny(s, SELF_PROMO)) return null;
        if (containsAny(s, EMPLOYMENT)) return null;

        boolean intent = inheritedIntent || containsAny(s, INTENT);
        if (!intent) return null;

        if (store.sitesEnabled() && isSiteCreation(s)) return Lead.Category.SITE;
        if (store.presentationsEnabled() && isPresentationCreation(s)) return Lead.Category.PRESENTATION;
        return null;
    }

    private boolean isSiteCreation(String s) {
        if (!containsAny(s, SITE_OBJECT)) return false;
        if (containsAny(s, COMPLEX_SITE)) return false;
        if (requiresForbiddenPlatform(s)) return false;
        if (requiresBrandWork(s)) return false;

        boolean explicitBuild = containsAny(s, SITE_BUILD_ACTION);
        boolean genericBuild = containsAny(s, new String[]{"сверст", "верстк", "под ключ", "с нуля"}) && containsAny(s, SITE_OBJECT);
        if (!explicitBuild && !genericBuild) return false;

        if (containsAny(s, DESIGN_ONLY_SITE) && !containsAny(s, new String[]{"сверст", "верстк", "собрать", "под ключ", "запуск"})) {
            return false;
        }

        if (containsAny(s, new String[]{"внести правки", "доработать сайт", "доработать лендинг", "исправить сайт",
                "изменить дизайн", "настроить сайт", "подключить", "seo", "сео"}) && !containsAny(s, new String[]{"с нуля", "под ключ"})) {
            return false;
        }
        return true;
    }

    private boolean isPresentationCreation(String s) {
        if (!containsAny(s, PRESENTATION_OBJECT)) return false;
        if (requiresBrandWork(s)) return false;

        boolean action = containsAny(s, PRESENTATION_ACTION);
        boolean clearProject = containsAny(s, new String[]{"слайд", "слайдов", "pptx", "powerpoint", "pdf"})
                && containsAny(s, new String[]{"нужно", "нужна", "ищу", "требуется", "задача", "сделать", "оформить"});
        return action || clearProject;
    }

    private boolean requiresForbiddenPlatform(String s) {
        String x = s
                .replace("не на тильде", "")
                .replace("не тильда", "")
                .replace("без тильды", "")
                .replace("без tilda", "")
                .replace("не tilda", "")
                .replace("не на wordpress", "")
                .replace("не wordpress", "")
                .replace("без wordpress", "")
                .replace("не на вордпресс", "")
                .replace("не вордпресс", "")
                .replace("без вордпресс", "");
        return containsAny(x, new String[]{"tilda", "тильд", "wordpress", "вордпресс", "word press"});
    }

    private boolean requiresBrandWork(String s) {
        String x = s
                .replace("логотип готов", "")
                .replace("лого готов", "")
                .replace("логотип уже есть", "")
                .replace("лого уже есть", "")
                .replace("фирменный стиль готов", "")
                .replace("фирменный стиль уже есть", "")
                .replace("есть фирменный стиль", "")
                .replace("по фирменному стилю", "")
                .replace("в фирменном стиле", "")
                .replace("гайдлайн готов", "")
                .replace("гайдлайны есть", "");
        return containsAny(x, new String[]{
                "создать логотип", "сделать логотип", "разработать логотип", "нужен логотип", "нужно лого",
                "айдентик", "брендбук", "разработать фирменный стиль", "создать фирменный стиль", "разработка фирменного стиля",
                "брендинг под ключ"
        });
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
            List<String> one = new ArrayList<>();
            one.add(text);
            return one;
        }

        List<String> parts = new ArrayList<>();
        int start = 0;
        for (Integer end : contactEnds) {
            if (end <= start) continue;
            String part = text.substring(start, end).trim();
            if (part.length() >= 20) parts.add(part);
            start = end;
        }
        if (start < text.length()) {
            String tail = text.substring(start).trim();
            if (tail.length() >= 20 && !parts.isEmpty()) {
                int last = parts.size() - 1;
                parts.set(last, parts.get(last) + " " + tail);
            }
        }
        return parts.isEmpty() ? java.util.Collections.singletonList(text) : parts;
    }

    private List<String> splitByPattern(String text, Pattern pattern) {
        String[] raw = pattern.split(text);
        List<String> out = new ArrayList<>();
        for (String part : raw) {
            String p = part.trim();
            if (p.length() >= 20) out.add(p);
        }
        return out;
    }

    private String chooseContact(String text, String channel) {
        Matcher m = HANDLE.matcher(text);
        String best = null;
        int bestScore = Integer.MIN_VALUE;
        String lower = text.toLowerCase(Locale.ROOT);
        while (m.find()) {
            String user = m.group(1);
            if (user.equalsIgnoreCase(channel) || user.toLowerCase(Locale.ROOT).endsWith("bot")) continue;
            int from = Math.max(0, m.start() - 90);
            int to = Math.min(text.length(), m.end() + 45);
            String around = lower.substring(from, to);
            int score = 0;
            if (around.contains("пишите") || around.contains("писать") || around.contains("напишите")) score += 8;
            if (around.contains("отклик") || around.contains("присылайте")) score += 7;
            if (around.contains("контакт") || around.contains("связ")) score += 6;
            if (around.contains("лс") || around.contains("личк")) score += 5;
            if (m.start() > text.length() * 0.50) score += 4;
            if (score > bestScore) {
                bestScore = score;
                best = user;
            }
        }
        return bestScore >= 2 ? best : null;
    }

    private String extractBudget(String text) {
        Matcher m = BUDGET.matcher(text);
        if (!m.find()) return "";
        return m.group(1).replace('\u00A0', ' ') + " " + m.group(2);
    }

    private boolean isFresh(Element message) {
        try {
            Element time = message.selectFirst("time[datetime]");
            if (time == null) return true;
            OffsetDateTime posted = OffsetDateTime.parse(time.attr("datetime"));
            long hours = Duration.between(posted.toInstant(), OffsetDateTime.now(ZoneOffset.UTC).toInstant()).toHours();
            return hours >= -2 && hours <= 72;
        } catch (Exception ignored) {
            return true;
        }
    }

    private static boolean containsAny(String s, String[] needles) {
        for (String n : needles) if (s.contains(n)) return true;
        return false;
    }

    private static int countCyrillic(String s) {
        int count = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if ((c >= 'а' && c <= 'я') || c == 'ё') count++;
        }
        return count;
    }

    public static String normalizeChannel(String raw) {
        if (raw == null) return "";
        String s = raw.trim();
        s = s.replace("https://t.me/s/", "").replace("https://t.me/", "");
        if (s.startsWith("@")) s = s.substring(1);
        int slash = s.indexOf('/');
        if (slash >= 0) s = s.substring(0, slash);
        return s.replaceAll("[^A-Za-z0-9_]", "");
    }
}
