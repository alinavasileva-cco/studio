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

    private static final String[] INTENT = {
            "ищу", "ищем", "нужен", "нужна", "нужны", "нужно", "требуется", "требуются",
            "задача", "заказ", "подрядчик", "исполнитель"
    };

    private static final String[] REJECT = {
            "ищу работу", "ищу клиентов", "оказываю услуги", "мои услуги", "создам сайт",
            "делаю сайты", "могу сделать сайт", "вакансия", "в штат", "полная занятость",
            "оформление по тк", "собеседование", "присылайте резюме", "отправляйте резюме",
            "зарплата", "резюме кандидата"
    };

    private static final String[] SIMPLE_SITE = {
            "сайт", "лендинг", "landing", "tilda", "тильд", "визитк", "одностранич"
    };

    private static final String[] COMPLEX_SITE = {
            "маркетплейс", "интернет-магазин", "интернет магазин", "мобильное приложение",
            "мобильного приложения", "backend", "бекенд", "личный кабинет", "fullstack",
            "фулстек", "saas", "сложный сервис", "сложного сервиса"
    };

    private static final String[] PRESENTATION = {
            "презентац", "pitch deck", "питч-дек", "питч дек", "презентацию для инвестор"
    };

    private static final String[] CONSULTING = {
            "бизнес-трек", "бизнес трек", "консульт", "стратегическая сесс", "стратегическую сесс",
            "стратегия бизнеса", "стратегию бизнеса", "рост бизнеса", "аудит бизнеса",
            "разобрать бизнес", "unit economics", "юнит-эконом", "p&l", "коммерческая стратегия",
            "коммерческую стратегию", "настроить продажи", "отдел продаж", "масштабирован"
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
            if (store.wasSent(postUrl)) continue;
            if (!isFresh(message)) continue;

            Element textNode = message.selectFirst("div.tgme_widget_message_text");
            if (textNode == null) continue;
            String text = textNode.text().trim();
            if (text.length() < 20) continue;

            Lead.Category category = classify(text, store);
            if (category == null) continue;

            String username = chooseContact(text, channel);
            if (username == null || username.isEmpty()) continue;

            String budget = extractBudget(text);
            out.add(new Lead(category, channel, postUrl, text, username, budget));
        }
        return out;
    }

    private Lead.Category classify(String text, LeadStore store) {
        String s = text.toLowerCase(Locale.ROOT);
        if (countCyrillic(s) < 15) return null;
        if (!containsAny(s, INTENT)) return null;
        if (containsAny(s, REJECT)) return null;

        if (store.sitesEnabled() && containsAny(s, SIMPLE_SITE) && !containsAny(s, COMPLEX_SITE)) {
            return Lead.Category.SITE;
        }
        if (store.presentationsEnabled() && containsAny(s, PRESENTATION)) {
            return Lead.Category.PRESENTATION;
        }
        if (store.consultingEnabled() && containsAny(s, CONSULTING)) {
            return Lead.Category.CONSULTING;
        }
        return null;
    }

    private String chooseContact(String text, String channel) {
        Matcher m = HANDLE.matcher(text);
        String best = null;
        int bestScore = Integer.MIN_VALUE;
        String lower = text.toLowerCase(Locale.ROOT);
        while (m.find()) {
            String user = m.group(1);
            if (user.equalsIgnoreCase(channel) || user.toLowerCase(Locale.ROOT).endsWith("bot")) continue;
            int from = Math.max(0, m.start() - 70);
            int to = Math.min(text.length(), m.end() + 35);
            String around = lower.substring(from, to);
            int score = 0;
            if (around.contains("пишите") || around.contains("писать") || around.contains("напишите")) score += 8;
            if (around.contains("отклик")) score += 7;
            if (around.contains("контакт") || around.contains("связ")) score += 5;
            if (around.contains("лс") || around.contains("личк")) score += 5;
            if (m.start() > text.length() * 0.6) score += 2;
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
