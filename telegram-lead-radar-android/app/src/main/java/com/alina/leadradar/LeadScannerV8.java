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
 * v8 scanner: only concrete quick/project orders for presentations and simple sites,
 * and only when a direct Telegram contact is present in the Telegram post itself.
 * No Kwork/YouDo/external marketplace links are accepted as a contact.
 */
public final class LeadScannerV8 {
    private static final Pattern HANDLE = Pattern.compile("@([A-Za-z0-9_]{5,32})");
    private static final Pattern USERNAME = Pattern.compile("^[A-Za-z0-9_]{5,32}$");
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
            "релокация", "собеседование", "резюме кандидата", "присылайте резюме", "отправляйте резюме",
            "ставка в месяц", "часов в неделю", "рабочий день", "офисный формат"
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

    private static final String[] RESPONSE_WORDS = {
            "пишите", "писать", "напишите", "отклик", "откликаться", "откликнуться", "контакт", "связь",
            "связаться", "телеграм", "telegram", "тг", "личку", "лс", "присылайте", "портфолио"
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

                    String username = chooseDirectTelegramContact(text, message, channel);
                    if (username == null || username.isEmpty()) continue;

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

            try {
                Thread.sleep(140L);
            } catch (InterruptedException e) {
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
        return role && containsAny(s, "удаленка", "удалёнка", "remote")
                && containsAny(s, "опыт", "команд", "проект");
    }

    private boolean isSiteOrder(String s) {
        if (!containsAny(s, "сайт", "лендинг", "landing", "одностранич")) return false;
        if (containsAny(s, COMPLEX_SITE)) return false;
        if (requiresForbiddenPlatform(s)) return false;
        if (requiresBrandWork(s)) return false;
        if (containsAny(s,
                "продаём готовые лендинги", "продаем готовые лендинги", "продаём лендинги", "продаем лендинги",
                "менеджер по продажам", "ваша задача: найти владельца бизнеса", "ваша задача — найти владельца бизнеса")) return false;
        if (containsAny(s, SITE_DESIGN_ONLY)) return false;
        return containsAny(s, SITE_BUILD);
    }

    private boolean isPresentationOrder(String s) {
        if (!containsAny(s, "презентац", "pitch deck", "питч-дек", "питч дек", "powerpoint", "pptx", "слайды")) return false;
        if (requiresBrandWork(s)) return false;
        if (containsAny(s, INCIDENTAL_PRESENTATION)
                && !containsAny(s, PRESENTATION_BUILD)
                && !containsAny(s, PRESENTATION_DIRECT_REQUEST)) return false;
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
        return containsAny(x,
                "создать логотип", "сделать логотип", "разработать логотип", "нужен логотип",
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
            List<String> one = new ArrayList<>();
            one.add(text);
            return one;
        }

        List<String> parts = new ArrayList<>();
        int start = 0;
        for (Integer end : contactEnds) {
            if (end <= start) continue;
            String part = text.substring(start, end).trim();
            if (part.length() >= 15) parts.add(part);
            start = end;
        }
        if (start < text.length() && !parts.isEmpty()) {
            String tail = text.substring(start).trim();
            if (tail.length() >= 15) {
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
            if (p.length() >= 15) out.add(p);
        }
        return out;
    }

    private String chooseDirectTelegramContact(String taskText, Element message, String channel) {
        String best = null;
        int bestScore = Integer.MIN_VALUE;
        String lower = taskText.toLowerCase(Locale.ROOT);

        Matcher m = HANDLE.matcher(taskText);
        while (m.find()) {
            String user = m.group(1);
            if (!isAllowedUsername(user, channel)) continue;
            int from = Math.max(0, m.start() - 120);
            int to = Math.min(taskText.length(), m.end() + 80);
            String around = lower.substring(from, to);
            int score = scoreContactContext(around);
            if (m.end() >= taskText.length() - 140) score += 3;
            if (around.contains("канал") || around.contains("подпис")) score -= 5;
            if (score > bestScore) {
                bestScore = score;
                best = user;
            }
        }

        Elements links = message.select("a[href]");
        for (Element a : links) {
            String user = usernameFromTelegramHref(a.attr("href"));
            if (!isAllowedUsername(user, channel)) continue;
            String context = (a.parent() == null ? a.text() : a.parent().text()).toLowerCase(Locale.ROOT);
            int score = scoreContactContext(context);
            String anchorText = a.text().toLowerCase(Locale.ROOT);
            if (anchorText.startsWith("@")) score += 3;
            if (containsAny(anchorText, "написать", "отклик", "контакт", "telegram", "телеграм")) score += 5;
            if (context.contains("канал") || context.contains("подпис")) score -= 5;
            if (score > bestScore) {
                bestScore = score;
                best = user;
            }
        }

        return bestScore >= 2 ? best : null;
    }

    private int scoreContactContext(String around) {
        int score = 0;
        for (String word : RESPONSE_WORDS) {
            if (around.contains(word)) score += 3;
        }
        if (around.contains("✍") || around.contains("👉") || around.contains("📩") || around.contains("📨")) score += 4;
        return score;
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
        if (marker < 0) {
            marker = lower.indexOf("telegram.me/");
            markerLen = 12;
        }
        if (marker < 0) return null;

        String path = h.substring(marker + markerLen);
        int q = path.indexOf('?');
        if (q >= 0) path = path.substring(0, q);
        int hash = path.indexOf('#');
        if (hash >= 0) path = path.substring(0, hash);
        while (path.startsWith("/")) path = path.substring(1);

        if (path.startsWith("s/") || path.startsWith("c/") || path.startsWith("+")
                || path.startsWith("joinchat/") || path.startsWith("share/") || path.startsWith("iv/")) return null;
        if (path.contains("/")) return null;
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
        try {
            return OffsetDateTime.parse(raw).toInstant();
        } catch (Exception ignored) {
            try { return Instant.parse(raw); } catch (Exception ignored2) { return null; }
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
        int slash = s.indexOf('/');
        if (slash >= 0) s = s.substring(0, slash);
        int q = s.indexOf('?');
        if (q >= 0) s = s.substring(0, q);
        return USERNAME.matcher(s).matches() ? s : "";
    }

    private boolean containsAny(String s, String... needles) {
        if (s == null || needles == null) return false;
        for (String n : needles) {
            if (n != null && !n.isEmpty() && s.contains(n)) return true;
        }
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
}
