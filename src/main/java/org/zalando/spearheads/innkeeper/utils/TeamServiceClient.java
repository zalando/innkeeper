package org.zalando.spearheads.innkeeper.utils;

import com.google.inject.BindingAnnotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * @author dpersa
 */
@Retention(RetentionPolicy.RUNTIME)
@BindingAnnotation
public @interface TeamServiceClient {
}
