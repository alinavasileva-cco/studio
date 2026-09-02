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
            "задача", "заказ", "подрядчик", "исполнитель", "специалист", "кто может", "кто сможет",
            "пишите", "напишите", "отклик"
    };

    private static final String[] SELF_PROMO = {
            "ищу работу", "ищу клиентов", "оказываю услуги", "мои услуги", "создам вам", "делаю сайты",
            "могу сделать сайт", "возьму заказ", "готов взять", "портфолио моих работ"
    };

    private static final String[] EMPLOYMENT = {
            "#вакансия", "вакансия", "в штат", "полная занятость", "частичная занятость", "оформление по тк",
            "собеседование", "присылайте резюме", "отправляйте резюме", "резюме кандидата", "зарплата",
            "обязанности:", "обязанности -", "требования:", "требования -", "условия:", "условия -",
            "график работы", "испытательный срок", "гражданство рф", "для женщин (от 18", "оплата от 60 000 рублей в месяц"
    };

    private static final String[] SITE_ACTIONS = {
            "сделать сайт", "создать сайт", "разработать сайт", "разработка сайта", "собрать сайт", "сверстать сайт",
            "доработать сайт", "доработка сайта", "редизайн сайта", "перенести сайт", "нужен сайт", "нужна разработка сайта",
            "сделать лендинг", "создать лендинг", "собрать лендинг", "разработать лендинг", "нужен лендинг", "нужна посадочная",
            "сайт-визит", "сайт визит", "одностраничный сайт", "одностраничник", "лендинг на tilda", "сайт на tilda",
            "лендинг на тильд", "сайт на тильд", "верстка на tilda", "верстка на тильд"
    };

    private static final String[] COMPLEX_SITE = {
            "маркетплейс", "интернет-магазин", "интернет магазин", "мобильное приложение", "мобильного приложения",
            "backend", "бекенд", "личный кабинет", "fullstack", "фулстек", "saas", "crm-система", "crm система"
    };

    private static final String[] PRESENTATION_ACTIONS = {
            "сделать презентац", "создать презентац", "собрать презентац", "оформить презентац", "дизайн презентац",
            "нужна презентац", "нужен дизайнер презентац", "ищу дизайнера презентац", "ищем дизайнера презентац",
            "pitch deck", "питч-дек", "питч дек", "презентацию для инвестор", "коммерческую презентац",
            "презентация для продаж", "презентацию для продаж"
    };

    private static final String[] AI_TERMS = {
            "нейросет", "искусственн", "chatgpt", "gpt", "midjourney", "миджорн", "stable diffusion", "stablediffusion",
            "runway", "kling", "sora", "flux", "comfyui", "leonardo ai", "нейрофото", "нейровидео", "ai-видео",
            "ai video", "ai-контент", "ai контент", "ai-аватар", "ai аватар", "нейроаватар", "промпт", "prompt"
    };

    private static final String[] GENERATION_TERMS = {
            "сгенерировать", "генерация изображ", "генерация картин", "генерация фото", "генерация видео", "генерация ролик",
            "генерация креатив", "генерация контент", "генерация персонаж", "генерация аватар", "генерация визуал",
            "создать нейро", "сделать нейро", "нейро фото", "нейро видео"
    };

    private static final String[] CONSULTING_STRICT = {
            "бизнес-трекер", "бизнес трекер", "бизнес-трекинг", "бизнес трекинг", "бизнес-консультант", "бизнес консультант",
            "консультант по бизнесу", "консультация по бизнесу", "стратегическая сессия", "стратегическую сессию",
            "аудит бизнеса", "разбор бизнеса", "разобрать бизнес", "диагностика бизнеса", "стратегия бизнеса",
            "стратегию бизнеса", "коммерческая стратегия", "коммерческую стратегию", "масштабирование бизнеса",
            "настроить отдел продаж", "построить отдел продаж", "юнит-экономика", "юнит экономика", "unit economics"
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

            List<String> tasks = splitTasks(fullText, channel);
            for (String task : tasks) {
                String text = task.replaceAll("[\\t ]+", " ").trim();
                if (text.length() < 20) continue;

                Lead.Category category = classify(text, store);
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

    private Lead.Category classify(String text, LeadStore store) {
        String s = text.toLowerCase(Locale.ROOT);
        if (countCyrillic(s) < 10) return null;
        if (!containsAny(s, INTENT)) return null;
        if (containsAny(s, SELF_PROMO)) return null;
        boolean employment = containsAny(s, EMPLOYMENT);

        if (store.sitesEnabled() && !employment && containsAny(s, SITE_ACTIONS) && !containsAny(s, COMPLEX_SITE)) {
            return Lead.Category.SITE;
        }

        if (store.presentationsEnabled() && !employment && containsAny(s, PRESENTATION_ACTIONS)) {
            return Lead.Category.PRESENTATION;
        }

        if (store.aiEnabled() && !employment && isAiTask(s)) {
            return Lead.Category.AI;
        }

        if (store.consultingEnabled() && !employment && containsAny(s, CONSULTING_STRICT)) {
            return Lead.Category.CONSULTING;
        }

        return null;
    }

    private boolean isAiTask(String s) {
        boolean explicitAi = containsAny(s, AI_TERMS) || hasAiWord(s);
        boolean generation = containsAny(s, GENERATION_TERMS);
        if (generation) return true;
        if (!explicitAi) return false;
        return containsAny(s, INTENT)
                && containsAny(s, new String[]{"сдел", "созда", "сгенер", "генерац", "настро", "обуч", "аватар", "видео",
                "изображ", "картин", "фото", "контент", "креатив", "визуал", "персонаж", "озвуч", "голос", "промпт", "prompt",
                "специалист", "эксперт", "разработ"});
    }

    private boolean hasAiWord(String s) {
        String padded = " " + s.replaceAll("[^a-zа-яё0-9]+", " ") + " ";
        return padded.contains(" ai ") || padded.contains(" ии ");
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
            if (around.contains("отклик")) score += 7;
            if (around.contains("контакт") || around.contains("связ")) score += 5;
            if (around.contains("лс") || around.contains("личк")) score += 5;
            if (m.start() > text.length() * 0.55) score += 3;
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
