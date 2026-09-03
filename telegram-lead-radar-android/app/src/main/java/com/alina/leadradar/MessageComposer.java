package com.alina.leadradar;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MessageComposer {
    private static final Pattern FOR_CONTEXT = Pattern.compile("(?iu)\\bдля\\s+([А-Яа-яЁёA-Za-z0-9«»\"'()№&+\\- ]{3,70})");

    private MessageComposer() {}

    public static String compose(Lead lead, String siteProfileUrl, String presentationPortfolioUrl) {
        int variant = Math.abs((lead.dedupKey == null ? lead.text : lead.dedupKey).hashCode()) % 4;

        if (lead.category == Lead.Category.SITE) {
            String link = clean(siteProfileUrl);
            String suffix = link.isEmpty() ? "" : "\n\nПортфолио: " + link;
            return siteMessage(lead, variant) + suffix;
        }
        if (lead.category == Lead.Category.PRESENTATION) {
            String link = clean(presentationPortfolioUrl);
            String suffix = link.isEmpty() ? "" : "\n\nПортфолио презентаций: " + link;
            return presentationMessage(lead, variant) + suffix;
        }
        return "";
    }

    // Backward-compatible overload for older call sites during migration.
    public static String compose(Lead lead, String profileUrl) {
        return compose(lead, profileUrl, profileUrl);
    }

    private static String siteMessage(Lead lead, int variant) {
        String s = lower(lead.text);
        String object;
        if (s.contains("сайт-визит")) object = "сайт-визитку";
        else if (s.contains("лендинг") || s.contains("landing")) object = "лендинг";
        else if (s.contains("одностранич")) object = "одностраничный сайт";
        else object = "сайт";

        String context = extractForContext(lead.text);
        if (!context.isEmpty()) object += " " + context;

        String opening;
        if (variant == 0) opening = "Здравствуйте! Увидела ваш запрос на " + object + ".";
        else if (variant == 1) opening = "Добрый день! Откликаюсь на задачу по созданию " + object + ".";
        else if (variant == 2) opening = "Здравствуйте! Увидела, что вам нужно сделать " + object + ".";
        else opening = "Здравствуйте! Нашла вашу задачу по созданию сайта и могу взять её в работу.";

        String material = "";
        if (containsAny(s, "дизайн готов", "макет готов", "макеты готовы")) {
            material = " Вижу, что макет уже готов — можно быстро перейти к сборке.";
        } else if (containsAny(s, "тз готов", "тз есть", "техническое задание")) {
            material = " Вижу, что ТЗ уже есть — это упростит оценку и старт.";
        } else if (containsAny(s, "тексты готовы", "контент готов", "материалы готовы")) {
            material = " Материалы уже готовы, поэтому можно быстро перейти к работе.";
        }

        String body = " Могу сделать под ключ: структура, аккуратная визуальная подача, адаптация под мобильные устройства и подготовка к запуску.";
        return opening + material + body + endingForConditions(s, variant);
    }

    private static String presentationMessage(Lead lead, int variant) {
        String s = lower(lead.text);
        String object;
        if (s.contains("инвестор")) object = "презентацию для инвесторов";
        else if (s.contains("pitch") || s.contains("питч")) object = "питч-дек";
        else if (s.contains("коммерческ")) object = "коммерческую презентацию";
        else if (s.contains("продаж")) object = "презентацию для продаж";
        else object = "презентацию";

        String context = extractForContext(lead.text);
        if (!context.isEmpty() && !object.toLowerCase(Locale.ROOT).contains("для")) object += " " + context;

        String opening;
        if (variant == 0) opening = "Здравствуйте! Увидела ваш запрос на " + object + ".";
        else if (variant == 1) opening = "Добрый день! Откликаюсь на вашу задачу по презентации.";
        else if (variant == 2) opening = "Здравствуйте! Увидела, что вам нужно собрать " + object + ".";
        else opening = "Здравствуйте! Нашла ваш запрос по презентации и могу взять задачу в работу.";

        String body = " Могу собрать структуру, привести материал к понятной логике, оформить визуально и подготовить финальный PowerPoint/PDF.";
        return opening + body + endingForConditions(s, variant);
    }

    private static String endingForConditions(String s, int variant) {
        boolean hasDeadline = containsAny(s, "срок", "дедлайн", "до завтра", "сегодня", "срочно");
        boolean hasBudget = containsAny(s, "бюджет", "руб", "₽");
        if (hasDeadline && hasBudget) return " Если задача ещё актуальна, могу оценить объём и подтвердить, укладываюсь ли в указанные условия.";
        if (hasDeadline) return " Если задача ещё актуальна, могу быстро оценить объём и подтвердить, укладываюсь ли в срок.";
        if (hasBudget) return " Если задача ещё актуальна, могу оценить объём и предложить вариант в рамках указанного бюджета.";
        return variant % 2 == 0
                ? " Если задача ещё актуальна, могу быстро оценить объём, сроки и стоимость."
                : " Если ещё актуально, пришлите детали — быстро оценю объём и предложу вариант по срокам и стоимости.";
    }

    private static String extractForContext(String text) {
        if (text == null) return "";
        Matcher m = FOR_CONTEXT.matcher(text.replace('\n', ' '));
        if (!m.find()) return "";
        String raw = m.group(1).trim();
        raw = raw.split("[.!?;,:]|(?:\\s+-\\s+)")[0].trim();
        String lower = raw.toLowerCase(Locale.ROOT);
        if (raw.length() < 3 || raw.length() > 55) return "";
        if (containsAny(lower, "отклик", "связ", "телеграм", "telegram", "резюме", "портфолио")) return "";
        return "для " + raw;
    }

    private static boolean containsAny(String s, String... values) {
        for (String v : values) if (s.contains(v)) return true;
        return false;
    }

    private static String lower(String s) {
        return s == null ? "" : s.toLowerCase(Locale.ROOT);
    }

    private static String clean(String s) {
        return s == null ? "" : s.trim();
    }
}
