package com.googlesource.gerrit.plugins.websession.flatfile;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import com.google.inject.BindingAnnotation;

import java.lang.annotation.Retention;

@Retention(RUNTIME)
@BindingAnnotation
@interface WebSessionDir {
}
