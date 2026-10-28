package com.zodiakk.projectservice.common.exception;

import lombok.experimental.UtilityClass;

@UtilityClass
public class ErrorMessages {

    public static final String PROJECT_NOT_FOUND = "Проект не найден с идентификатором: %s";

    public static final String PROJECT_CODE_ALREADY_EXISTS = "Проект уже существует с кодом: %s";

    public static final String INVALID_PROJECT_DATES = "Дата окончания проекта не может быть раньше даты начала";

    public static final String INVALID_PROJECT_STATUS_TRANSITION = "Недопустимый переход статуса проекта: %s -> %s";

    public static final String PROJECT_ROLE_NOT_FOUND = "Проектная роль не найдена с идентификатором: %s";

    public static final String PROJECT_ROLE_CODE_ALREADY_EXISTS = "Проектная роль уже существует с кодом: %s";

    public static final String PROJECT_ROLE_IN_USE = "Проектную роль нельзя удалить, так как она используется в назначениях: %s";

    public static final String PROJECT_ROLE_INACTIVE = "Проектная роль неактивна: %s";

    public static final String PROJECT_ASSIGNMENT_NOT_FOUND = "Назначение на проект не найдено с идентификатором: %s";

    public static final String PROJECT_ASSIGNMENT_ALREADY_EXISTS = "Сотрудник %s уже назначен на проект %s";

    public static final String INVALID_ASSIGNMENT_DATES = "Дата окончания назначения не может быть раньше даты начала";

    public static final String INVALID_ALLOCATION_PERCENT = "Процент загрузки должен быть в диапазоне от 0 до 100: %s";

    public static final String PROJECT_MILESTONE_NOT_FOUND = "Веха проекта не найдена с идентификатором: %s";

    public static final String PROJECT_LINK_NOT_FOUND = "Ссылка проекта не найдена с идентификатором: %s";

    public static final String PROJECT_BOARD_NOT_FOUND = "Доска проекта не найдена с идентификатором: %s";

    public static final String PROJECT_BOARD_ALREADY_EXISTS = "Для проекта уже существует доска: %s";

    public static final String BOARD_COLUMN_NOT_FOUND = "Колонка доски не найдена с идентификатором: %s";

    public static final String BOARD_COLUMN_CODE_ALREADY_EXISTS = "Колонка доски уже существует с кодом: %s";

    public static final String BOARD_COLUMN_POSITION_ALREADY_EXISTS = "Позиция колонки уже занята: %s";

    public static final String BOARD_COLUMN_ORDER_MISMATCH = "Набор колонок в запросе не совпадает с колонками доски";

    public static final String BOARD_COLUMN_IN_USE = "Колонку нельзя удалить, так как в ней есть задачи: %s";

    public static final String PROJECT_TASK_NOT_FOUND = "Задача не найдена с идентификатором: %s";

    public static final String TASK_TYPE_NOT_FOUND = "Тип задачи не найден с идентификатором: %s";

    public static final String TASK_TYPE_CODE_ALREADY_EXISTS = "Тип задачи уже существует с кодом: %s";

    public static final String TASK_TYPE_IN_USE = "Тип задачи нельзя удалить, так как он используется задачами: %s";

    public static final String TASK_TYPE_INACTIVE = "Тип задачи неактивен: %s";

    public static final String TASK_ARCHIVED = "Задача уже архивирована: %s";

    public static final String TASK_HAS_CHILDREN = "Задачу нельзя удалить, пока у неё есть дочерние задачи: %s";

    public static final String INVALID_PARENT_TASK_TYPE = "Недопустимый тип дочерней задачи для родителя %s: %s";

    public static final String CROSS_PROJECT_PARENT = "Родительская и дочерняя задачи должны принадлежать одному проекту";

    public static final String TASK_HIERARCHY_CYCLE = "Иерархия задач не может содержать цикл";

    public static final String TASK_BLOCKED = "Задача заблокирована незавершёнными зависимостями";

    public static final String TASK_RELATION_NOT_FOUND = "Связь задач не найдена с идентификатором: %s";

    public static final String TASK_RELATION_ALREADY_EXISTS = "Связь задач уже существует";

    public static final String CROSS_PROJECT_RELATION = "Связанные задачи должны принадлежать одному проекту";

    public static final String TASK_DEPENDENCY_CYCLE = "Связь создаёт цикл в графе зависимостей";

    public static final String TASK_ASSIGNEE_NOT_FOUND = "Исполнитель задачи не найден с идентификатором: %s";

    public static final String TASK_ASSIGNEE_ALREADY_EXISTS = "Сотрудник уже назначен исполнителем задачи: %s";

    public static final String INVALID_TASK_ASSIGNEE = "Назначение %s не относится к проекту задачи";

    public static final String TASK_COMMENT_NOT_FOUND = "Комментарий задачи не найден с идентификатором: %s";

    public static final String TASK_SKILL_NOT_FOUND = "Навык задачи не найден с идентификатором: %s";

    public static final String TASK_SKILL_ALREADY_EXISTS = "Навык уже добавлен к задаче: %s";

    public static final String TASK_ACTIVITY_NOT_FOUND = "Запись истории задачи не найдена с идентификатором: %s";

    public static final String EMPLOYEE_REFERENCE_NOT_FOUND = "Сотрудник не найден с идентификатором: %s";

    public static final String EMPLOYEE_REFERENCE_INACTIVE = "Сотрудник неактивен: %s";

    public static final String EMPLOYEE_SERVICE_UNAVAILABLE = "Сервис сотрудников недоступен. Попробуйте позднее";

    public static final String CONCURRENT_MODIFICATION = "Объект был изменён другим пользователем. Обновите данные и повторите попытку";

    public static final String UNSUPPORTED_EVENT_TYPE = "Неподдерживаемый тип события: %s";

    public static final String OUTBOX_EVENT_SERIALIZATION_FAILED = "Не удалось сериализовать outbox-событие: %s";

    public static final String INTERNAL_SERVER_ERROR = "Внутренняя ошибка сервера. Попробуйте позднее.";

    public static final String DATA_INTEGRITY_VIOLATION = "Нарушено ограничение целостности данных";

    public static final String INVALID_REQUEST_BODY = "Некорректное тело запроса";

    public static final String RESOURCE_NOT_FOUND = "Запрошенный ресурс не найден";

    public static final String METHOD_NOT_ALLOWED = "Метод запроса не поддерживается";

    public static final String UNSUPPORTED_MEDIA_TYPE = "Неподдерживаемый тип содержимого запроса";

    public static final String INVALID_PARAMETER = "Некорректное значение параметра: %s";

    public static final String MISSING_PARAMETER = "Обязательный параметр отсутствует: %s";

    public static final String ACCESS_DENIED = "Доступ запрещён";

    public static final String UNAUTHORIZED = "Требуется аутентификация";

    public static final String INVALID_INTERNAL_API_KEY = "Некорректный внутренний API-ключ";
}
