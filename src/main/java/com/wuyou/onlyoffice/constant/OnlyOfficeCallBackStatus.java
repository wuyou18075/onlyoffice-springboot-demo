package cn.superlu.onlyoffice.constant;

public enum OnlyOfficeCallBackStatus {
    SUCCESS(0, "通用"),
    EDIT(1, "正在编辑文档"),
    READY_TO_SAVE(2, "文档已准备好保存"),
    SAVE_ERROR(3, "发生文档保存错误"),
    CLOSE_NO_CHANGE(4, "文档已关闭，没有任何更改"),
    SAVE(6, "保存"),
    FORCE_SAVE_ERROR(7, "强制保存文档时发生错误"),

    ;

    private final Integer code;
    private final String message;

    OnlyOfficeCallBackStatus(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

    public boolean equals(Integer code) {
        return this.code.equals(code);
    }
}
