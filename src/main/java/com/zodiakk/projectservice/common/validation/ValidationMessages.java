package com.zodiakk.projectservice.common.validation;

public final class ValidationMessages {

    private ValidationMessages() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static final String ACTION_REQUIRED = "Действие обязательно для заполнения";

    public static final String ACTION_SIZE = "Действие должно содержать не более {max} символов";

    public static final String ASSIGNEE_IDS_REQUIRED = "Идентификаторы исполнителей обязательны для заполнения";

    public static final String ASSIGNMENT_ID_REQUIRED = "Идентификатор назначения обязателен для заполнения";

    public static final String AUTHOR_EMPLOYEE_ID_REQUIRED = "Идентификатор автора обязателен для заполнения";

    public static final String BOARD_ID_REQUIRED = "Идентификатор доски обязателен для заполнения";

    public static final String CODE_REQUIRED = "Код обязателен для заполнения";

    public static final String CODE_SIZE = "Код должен содержать не более {max} символов";

    public static final String COLUMN_IDS_NOT_EMPTY = "Список «Идентификаторы колонок» должен содержать хотя бы один элемент";

    public static final String COLUMN_ID_REQUIRED = "Идентификатор колонки обязателен для заполнения";

    public static final String COMMENT_SIZE = "Комментарий должен содержать не более {max} символов";

    public static final String CONTENT_REQUIRED = "Содержимое обязательно для заполнения";

    public static final String CONTENT_SIZE = "Содержимое должно содержать не более {max} символов";

    public static final String EMPLOYEE_ID_REQUIRED = "Идентификатор сотрудника обязателен для заполнения";

    public static final String EXPECTED_VERSION_REQUIRED = "Ожидаемая версия обязательна для заполнения";

    public static final String IS_INITIAL_REQUIRED = "Флаг начальной колонки обязателен для заполнения";

    public static final String IS_TERMINAL_REQUIRED = "Флаг конечной колонки обязателен для заполнения";

    public static final String LABEL_REQUIRED = "Метка обязательна для заполнения";

    public static final String LABEL_SIZE = "Метка должна содержать не более {max} символов";

    public static final String NAME_REQUIRED = "Название обязательно для заполнения";

    public static final String NAME_SIZE = "Название должно содержать не более {max} символов";

    public static final String PAGE_MAX_SIZE = "Размер страницы не может превышать {value}";

    public static final String PAGE_MIN_SIZE = "Размер страницы должен быть больше нуля";

    public static final String PAGE_NUMBER_MIN = "Номер страницы не может быть отрицательным";

    public static final String POSITION_REQUIRED = "Позиция обязательна для заполнения";

    public static final String PRIORITY_REQUIRED = "Приоритет обязателен для заполнения";

    public static final String PROJECT_ID_REQUIRED = "Идентификатор проекта обязателен для заполнения";

    public static final String PROJECT_ROLE_ID_REQUIRED = "Идентификатор проектной роли обязателен для заполнения";

    public static final String RELATION_TYPE_REQUIRED = "Тип связи обязателен для заполнения";

    public static final String SKILL_ID_REQUIRED = "Идентификатор навыка обязателен для заполнения";

    public static final String SOURCE_TASK_ID_REQUIRED = "Идентификатор исходной задачи обязателен для заполнения";

    public static final String START_DATE_REQUIRED = "Дата начала обязательна для заполнения";

    public static final String STATUS_REQUIRED = "Статус обязателен для заполнения";

    public static final String STATUS_SIZE = "Статус должен содержать не более {max} символов";

    public static final String TARGET_COLUMN_ID_REQUIRED = "Идентификатор целевой колонки обязателен для заполнения";

    public static final String TARGET_TASK_ID_REQUIRED = "Идентификатор целевой задачи обязателен для заполнения";

    public static final String TASK_ID_REQUIRED = "Идентификатор задачи обязателен для заполнения";

    public static final String TASK_TYPE_ID_REQUIRED = "Идентификатор типа задачи обязателен для заполнения";

    public static final String TITLE_REQUIRED = "Заголовок обязателен для заполнения";

    public static final String TITLE_SIZE = "Заголовок должен содержать не более {max} символов";

    public static final String URL_REQUIRED = "Ссылка обязательна для заполнения";

    public static final String URL_SIZE = "Ссылка должна содержать не более {max} символов";
}
