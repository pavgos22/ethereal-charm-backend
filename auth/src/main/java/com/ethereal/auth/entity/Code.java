package com.ethereal.auth.entity;

public enum Code {
    SUCCESS("Operacja zakończona sukcesem"),
    PERMIT("Przyznano dostep"),
    A1("Podany uzytkownik o danej nazwie nie istnieje lub nie aktywował konta"),
    A2("Podane dane są nieprawidłowe"),
    A3("Wskazany token jest pusty lub nieważny"),
    A4("Użytkownik o podanej nazwie już istnieje"),
    A5("Użytkownik o podanmym mailu już istnieje"),
    A6("Użytkownik nie istnieje"),
    A7("Użytkownik jest zablokowany");

    public final String label;

    private Code(String label) {
        this.label = label;
    }
}
