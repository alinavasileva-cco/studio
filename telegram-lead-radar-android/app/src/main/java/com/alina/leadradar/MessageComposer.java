package com.alina.leadradar;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MessageComposer {
    private static final Pattern FOR_CONTEXT = Pattern.compile("(?iu)\\bдля\\s+([А-Яа-яЁёA-Za-z0-9«»\"'()№&+\\- ]{3,70})");

    private MessageComposer() {}

    public static String compose(Lead lead, String profileUrl) {
        String link = profileUrl == null ? "" : profileUrl.trim();
        String suffix = link.isEmpty() ? "" : "\n\nПортфолио: " + link;
        int variant = Math.abs((lead.dedupKey == null ? lead.text : lead.dedupKey).hashCode()) % 4;

        switch (lead.category) {
            case SITE:
                return siteMessage(lead, variant) + suffix;
            case PRESENTATION:
                return presentationMessage(lead, variant) + suffix;
            case AI:
                return aiMessage(lead, variant) + suffix;
            case CONSULTING:
            default:
                return consultingMessage(lead, variant) + suffix;
        }
    }

    private static String siteMessage(Lead lead, int variant) {
        String s = lower(lead.text);
        String object;
        if (s.contains("визитк")) object = "сайт-визитку";
        else if (s.contains("лендинг") || s.contains("landing")) object = "лендинг";
        else if (s.contains("одностранич")) object = "одностраничный сайт";
        else if (s.contains("tilda") || s.contains("тильд")) object = "сайт на Tilda";
        else object = "сайт";

        String context = extractForContext(lead.text);
        if (!context.isEmpty()) object += " " + context;

        String opening;
        if (variant == 0) opening = "Здравствуйте! Увидела ваш запрос на " + object + ".";
        else if (variant == 1) opening = "Добрый день! Откликаюсь на задачу по разработке — " + object + ".";
        else if (variant == 2) opening = "Здравствуйте! Увидела, что вам нужно сделать " + object + ".";
        else opening = "Здравствуйте! Нашла вашу задачу по сайту и могу взять её в работу.";

        String material = "";
        if (containsAny(s, "дизайн готов", "макет готов", "макеты готовы")) material = " Вижу, что макеты уже готовы — можно сразу переходить к сборке.";
        else if (containsAny(s, "тз готов", "тз есть", "техническое задание")) material = " Вижу, что ТЗ уже есть — это упростит оценку и старт.";
        else if (containsAny(s, "тексты готовы", "контент готов", "материалы готовы")) material = " Материалы уже готовы, поэтому можно быстро перейти к сборке.";

        String body = (s.contains("tilda") || s.contains("тильд"))
                ? " Могу собрать страницу на Tilda, адаптировать под мобильные устройства и подготовить к запуску."
                : " Могу сделать под ключ: структура, аккуратный дизайн, мобильная версия и запуск.";
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

        String body = " Могу собрать структуру, привести текст к понятной логике, оформить визуально и подготовить финальный PowerPoint/PDF.";
        return opening + body + endingForConditions(s, variant);
    }

    private static String aiMessage(Lead lead, int variant) {
        String s = lower(lead.text);
        String task;
        if (containsAny(s, "видео", "ролик", "нейровидео", "ai-видео", "ai video", "kling", "runway", "sora")) task = "AI-видео";
        else if (containsAny(s, "аватар", "персонаж")) task = "AI-аватар / персонажа";
        else if (containsAny(s, "изображ", "картин", "фото", "midjourney", "миджорн", "stable diffusion", "flux")) task = "генерацию изображений";
        else if (containsAny(s, "контент", "креатив", "визуал")) task = "AI-контент и креативы";
        else if (containsAny(s, "промпт", "prompt")) task = "работу с промптами и нейросетями";
        else task = "задачу с AI / нейросетями";

        String opening;
        if (variant == 0) opening = "Здравствуйте! Увидела ваш запрос на " + task + ".";
        else if (variant == 1) opening = "Добрый день! Откликаюсь на вашу задачу по AI — " + task + ".";
        else if (variant == 2) opening = "Здравствуйте! Увидела, что вам нужен специалист на " + task + ".";
        else opening = "Здравствуйте! Нашла вашу задачу по нейросетям и могу подключиться.";

        String body;
        if (task.contains("видео")) body = " Могу собрать подход к генерации, подобрать инструменты, сделать сцены и довести результат до готового ролика.";
        else if (task.contains("изображ")) body = " Могу подобрать подходящий стек нейросетей, собрать промпты и довести визуалы до нужной стилистики.";
        else if (task.contains("аватар")) body = " Могу подобрать инструменты, собрать образ и сделать серию генераций в единой стилистике.";
        else body = " Могу подобрать подходящий инструмент, собрать рабочий процесс и довести результат до готового материала.";
        return opening + body + endingForConditions(s, variant);
    }

    private static String consultingMessage(Lead lead, int variant) {
        String s = lower(lead.text);
        String topic;
        if (containsAny(s, "отдел продаж", "коммерческ")) topic = "продажам и коммерческой функции";
        else if (containsAny(s, "юнит", "unit economics", "финанс")) topic = "экономике и управленческой аналитике";
        else if (containsAny(s, "масштаб", "рост бизнеса")) topic = "росту и масштабированию бизнеса";
        else if (containsAny(s, "стратег")) topic = "стратегии бизнеса";
        else topic = "разбору бизнеса";

        String opening = variant % 2 == 0
                ? "Здравствуйте! Увидела ваш запрос по " + topic + "."
                : "Добрый день! Откликаюсь на ваш запрос по " + topic + ".";
        String body = " Работаю с диагностикой бизнеса, стратегией роста, коммерческой функцией, P&L и настройкой процессов.";
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
}
