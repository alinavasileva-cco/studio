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
        int variant = Math.abs((lead.postUrl == null ? lead.text : lead.postUrl).hashCode()) % 4;

        switch (lead.category) {
            case SITE:
                return siteMessage(lead, variant) + suffix;
            case PRESENTATION:
                return presentationMessage(lead, variant) + suffix;
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
        else if (s.contains("tilda") || s.contains("тильд")) object = "небольшой сайт на Tilda";
        else object = "небольшой сайт";

        String context = extractForContext(lead.text);
        if (!context.isEmpty()) object += " " + context;

        String material = "";
        if (containsAny(s, "дизайн готов", "дизайн уже", "макет готов", "макеты готовы")) {
            material = " Вижу, что дизайн уже подготовлен, поэтому можно сразу переходить к сборке.";
        } else if (containsAny(s, "тз готов", "тз есть", "техническое задание готов")) {
            material = " Вижу, что ТЗ уже есть — это ускорит оценку и старт.";
        } else if (containsAny(s, "тексты готовы", "контент готов", "материалы готовы")) {
            material = " Вижу, что материалы уже готовы — можно быстро перейти к сборке.";
        }

        String opening;
        if (variant == 0) opening = "Здравствуйте! Увидела ваш запрос на " + object + ".";
        else if (variant == 1) opening = "Здравствуйте! Нашла вашу заявку: нужен " + normalizeGender(object) + ".";
        else if (variant == 2) opening = "Добрый день! Увидела, что вы ищете исполнителя на " + object + ".";
        else opening = "Здравствуйте! Откликаюсь на ваш запрос по задаче — " + object + ".";

        String body;
        if (s.contains("tilda") || s.contains("тильд")) {
            body = " Могу собрать аккуратную страницу на Tilda, адаптировать под мобильные устройства и подготовить к запуску.";
        } else {
            body = " Могу сделать под ключ: структура, аккуратный дизайн, мобильная версия и запуск.";
        }

        String ending = endingForSite(variant, s);
        return opening + material + body + ending;
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

        String material = "";
        if (containsAny(s, "текст готов", "тексты готовы", "материалы готовы", "контент готов")) {
            material = " Вижу, что исходные материалы уже есть — можно сразу собирать структуру и визуальную подачу.";
        }

        String opening;
        if (variant == 0) opening = "Здравствуйте! Увидела ваш запрос на " + object + ".";
        else if (variant == 1) opening = "Добрый день! Откликаюсь на вашу задачу по презентации — " + object + ".";
        else if (variant == 2) opening = "Здравствуйте! Увидела, что вам нужно собрать " + object + ".";
        else opening = "Здравствуйте! Нашла ваш запрос по презентации и могу взять задачу в работу.";

        String body = " Могу сделать под ключ: логика и структура, работа с текстом, визуальная подача, единый дизайн и финальный PowerPoint/PDF.";
        String ending = variant % 2 == 0
                ? " Если задача ещё актуальна, могу быстро оценить объём и сроки по вашим материалам."
                : " Если ещё актуально, пришлите исходники — посмотрю и предложу вариант по объёму, срокам и стоимости.";
        return opening + material + body + ending;
    }

    private static String consultingMessage(Lead lead, int variant) {
        String s = lower(lead.text);
        String topic;
        if (containsAny(s, "отдел продаж", "продажи", "коммерческ")) topic = "продажам и коммерческой функции";
        else if (containsAny(s, "p&l", "юнит-эконом", "unit economics", "финанс")) topic = "P&L, экономике и управленческой аналитике";
        else if (containsAny(s, "масштаб", "рост бизнеса", "рост компании")) topic = "росту и масштабированию бизнеса";
        else if (containsAny(s, "стратег")) topic = "стратегии бизнеса";
        else if (containsAny(s, "процесс")) topic = "бизнес-процессам и управлению";
        else topic = "бизнес-задаче";

        String opening;
        if (variant == 0) opening = "Здравствуйте! Увидела ваш запрос по " + topic + ".";
        else if (variant == 1) opening = "Добрый день! Откликаюсь на ваш запрос по " + topic + ".";
        else if (variant == 2) opening = "Здравствуйте! Нашла вашу заявку на консультацию по " + topic + ".";
        else opening = "Здравствуйте! Увидела, что вы ищете помощь по " + topic + ".";

        String body = " Работаю с диагностикой бизнеса, стратегией роста, коммерческой функцией, P&L и настройкой процессов.";
        String ending = variant % 2 == 0
                ? " Если запрос ещё актуален, можем коротко обсудить ситуацию и я предложу подходящий формат работы."
                : " Если задача ещё актуальна, напишите, что сейчас является основной точкой проблемы — предложу формат работы без лишней теории.";
        return opening + body + ending;
    }

    private static String endingForSite(int variant, String s) {
        boolean hasDeadline = containsAny(s, "срок", "до ", "дедлайн");
        boolean hasBudget = containsAny(s, "бюджет", "руб", "₽");
        if (hasDeadline && hasBudget) {
            return " Если задача ещё актуальна, могу оценить объём по описанию и подтвердить, укладываюсь ли в указанные условия.";
        }
        if (hasDeadline) {
            return " Если задача ещё актуальна, могу быстро оценить объём и подтвердить, укладываюсь ли в указанный срок.";
        }
        if (hasBudget) {
            return " Если задача ещё актуальна, могу оценить объём и предложить оптимальный вариант в рамках указанного бюджета.";
        }
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

    private static String normalizeGender(String object) {
        if (object.startsWith("сайт") || object.startsWith("лендинг") || object.startsWith("одностраничный")) return object;
        return object;
    }

    private static boolean containsAny(String s, String... values) {
        for (String v : values) if (s.contains(v)) return true;
        return false;
    }

    private static String lower(String s) {
        return s == null ? "" : s.toLowerCase(Locale.ROOT);
    }
}
