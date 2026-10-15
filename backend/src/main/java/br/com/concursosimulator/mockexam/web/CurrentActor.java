package br.com.concursosimulator.mockexam.web;

import java.lang.annotation.*;

@Target(ElementType.PARAMETER) @Retention(RetentionPolicy.RUNTIME)
public @interface CurrentActor {}
