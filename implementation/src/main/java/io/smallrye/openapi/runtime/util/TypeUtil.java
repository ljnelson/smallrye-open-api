/*
 * Copyright 2018 Red Hat, Inc, and individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.smallrye.openapi.runtime.util;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.validation.constraints.NotNull;

import org.eclipse.microprofile.openapi.models.media.Schema.SchemaType;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationTarget.Kind;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodParameterInfo;
import org.jboss.jandex.PrimitiveType;
import org.jboss.jandex.Type;
import org.jboss.jandex.WildcardType;

import io.smallrye.openapi.api.OpenApiConstants;

/**
 * @author Marc Savy {@literal <marc@rhymewithgravy.com>}
 */
public class TypeUtil {

    private static final DotName DOTNAME_OBJECT = DotName.createSimple(Object.class.getName());
    private static final Type OBJECT_TYPE = Type.create(DOTNAME_OBJECT, Type.Kind.CLASS);
    private static final TypeWithFormat STRING_FORMAT = new TypeWithFormat(SchemaType.STRING, DataFormat.NONE);
    private static final TypeWithFormat BYTE_FORMAT = new TypeWithFormat(SchemaType.STRING, DataFormat.BYTE);
    private static final TypeWithFormat CHAR_FORMAT = new TypeWithFormat(SchemaType.STRING, DataFormat.BYTE);
    private static final TypeWithFormat NUMBER_FORMAT = new TypeWithFormat(SchemaType.NUMBER, DataFormat.NONE); // We can't immediately tell if it's int, float, etc.
    private static final TypeWithFormat BIGDECIMAL_FORMAT = new TypeWithFormat(SchemaType.NUMBER, DataFormat.NONE);
    private static final TypeWithFormat DOUBLE_FORMAT = new TypeWithFormat(SchemaType.NUMBER, DataFormat.DOUBLE);
    private static final TypeWithFormat FLOAT_FORMAT = new TypeWithFormat(SchemaType.NUMBER, DataFormat.FLOAT);
    private static final TypeWithFormat BIGINTEGER_FORMAT = new TypeWithFormat(SchemaType.INTEGER, DataFormat.NONE);
    private static final TypeWithFormat INTEGER_FORMAT = new TypeWithFormat(SchemaType.INTEGER, DataFormat.INT32);
    private static final TypeWithFormat LONG_FORMAT = new TypeWithFormat(SchemaType.INTEGER, DataFormat.INT64);
    private static final TypeWithFormat SHORT_FORMAT = new TypeWithFormat(SchemaType.INTEGER, DataFormat.NONE);
    private static final TypeWithFormat BOOLEAN_FORMAT = new TypeWithFormat(SchemaType.BOOLEAN, DataFormat.NONE);
    // SPECIAL FORMATS
    private static final TypeWithFormat ARRAY_FORMAT = new TypeWithFormat(SchemaType.ARRAY, DataFormat.NONE);
    private static final TypeWithFormat OBJECT_FORMAT = new TypeWithFormat(SchemaType.OBJECT, DataFormat.NONE);
    private static final TypeWithFormat DATE_FORMAT = new TypeWithFormat(SchemaType.STRING, DataFormat.DATE);
    private static final TypeWithFormat DATE_TIME_FORMAT = new TypeWithFormat(SchemaType.STRING, DataFormat.DATE_TIME);

    private static final Map<DotName, TypeWithFormat> TYPE_MAP = new LinkedHashMap<>();

    // https://github.com/OAI/OpenAPI-Specification/blob/master/versions/3.0.0.md#dataTypeFormat
    static {
        // String
        TYPE_MAP.put(DotName.createSimple(String.class.getName()), STRING_FORMAT);
        TYPE_MAP.put(DotName.createSimple(StringBuffer.class.getName()), STRING_FORMAT);
        TYPE_MAP.put(DotName.createSimple(StringBuilder.class.getName()), STRING_FORMAT);
        TYPE_MAP.put(DotName.createSimple(CharSequence.class.getName()), STRING_FORMAT);

        // B64 String
        TYPE_MAP.put(DotName.createSimple(Byte.class.getName()), BYTE_FORMAT);
        TYPE_MAP.put(DotName.createSimple(byte.class.getName()), BYTE_FORMAT);
        TYPE_MAP.put(DotName.createSimple(Character.class.getName()), CHAR_FORMAT);
        TYPE_MAP.put(DotName.createSimple(char.class.getName()), CHAR_FORMAT);

        // Number
        TYPE_MAP.put(DotName.createSimple(Number.class.getName()), NUMBER_FORMAT);

        // Decimal
        TYPE_MAP.put(DotName.createSimple(BigDecimal.class.getName()), BIGDECIMAL_FORMAT);
        TYPE_MAP.put(DotName.createSimple(Double.class.getName()), DOUBLE_FORMAT);
        TYPE_MAP.put(DotName.createSimple(double.class.getName()), DOUBLE_FORMAT);
        TYPE_MAP.put(DotName.createSimple(Float.class.getName()), FLOAT_FORMAT);
        TYPE_MAP.put(DotName.createSimple(float.class.getName()), FLOAT_FORMAT);

        // Integer
        TYPE_MAP.put(DotName.createSimple(BigInteger.class.getName()), BIGINTEGER_FORMAT);
        TYPE_MAP.put(DotName.createSimple(Integer.class.getName()), INTEGER_FORMAT);
        TYPE_MAP.put(DotName.createSimple(int.class.getName()), INTEGER_FORMAT);
        TYPE_MAP.put(DotName.createSimple(Long.class.getName()), LONG_FORMAT);
        TYPE_MAP.put(DotName.createSimple(long.class.getName()), LONG_FORMAT);
        TYPE_MAP.put(DotName.createSimple(Short.class.getName()), SHORT_FORMAT);
        TYPE_MAP.put(DotName.createSimple(short.class.getName()), SHORT_FORMAT);

        // Boolean
        TYPE_MAP.put(DotName.createSimple(Boolean.class.getName()), BOOLEAN_FORMAT);
        TYPE_MAP.put(DotName.createSimple(boolean.class.getName()), BOOLEAN_FORMAT);

        // Date
        TYPE_MAP.put(DotName.createSimple(Date.class.getName()), DATE_FORMAT);
        TYPE_MAP.put(DotName.createSimple(java.sql.Date.class.getName()), DATE_FORMAT);
        TYPE_MAP.put(DotName.createSimple(java.time.LocalDate.class.getName()), DATE_FORMAT);

        // Date Time
        TYPE_MAP.put(DotName.createSimple(java.time.LocalDateTime.class.getName()), DATE_TIME_FORMAT);
        TYPE_MAP.put(DotName.createSimple(java.time.ZonedDateTime.class.getName()), DATE_TIME_FORMAT);
        TYPE_MAP.put(DotName.createSimple(java.time.OffsetDateTime.class.getName()), DATE_TIME_FORMAT);
    }

    private TypeUtil() {
    }

    public static TypeWithFormat getTypeFormat(PrimitiveType primitiveType) {
        return TYPE_MAP.get(primitiveType.name());
    }

    // TODO: consider additional checks for Number interface?
    public static TypeWithFormat getTypeFormat(Type classType) {
        if (classType.kind() == Type.Kind.ARRAY) {
            return arrayFormat();
        } else {
            return Optional
                    .ofNullable(TYPE_MAP.get(getName(classType))) // TODO we could fall back onto tests for interfaces such as CharSequence.
                    // Otherwise it's some object without a well-known format mapping
                    .orElse(objectFormat());
        }
    }

    public static TypeWithFormat arrayFormat() {
        return ARRAY_FORMAT;
    }

    public static TypeWithFormat objectFormat() {
        return OBJECT_FORMAT;
    }

    public static Class<?> getClass(Type type) throws ClassNotFoundException {
        return getClass(getName(type).toString());
    }

    public static Class<?> getClass(String name) throws ClassNotFoundException {
        return Class.forName(name);
    }

    /**
     * Test whether testSubject is an instanceof type test.
     * <p>
     * For example, test whether List is a Collection.
     * <p>
     * Attempts to work with both Jandex and using standard class.
     *
     * @param index Jandex index
     * @param testSubject type to test
     * @param testObject type to test against
     * @return true if is of type
     */
    public static boolean isA(IndexView index, Type testSubject, Type testObject) {
        // First, look in Jandex, as target might not be in our classloader
        ClassInfo subJandexKlazz = index.getClassByName(getName(testSubject));

        if (subJandexKlazz != null) {
            return subJandexKlazz.interfaceNames().contains(getName(testObject)) || hasSuper(index, subJandexKlazz, testObject);
        } else {
            try {
                Class<?> subjectKlazz = TypeUtil.getClass(testSubject);
                Class<?> objectKlazz = TypeUtil.getClass(testObject);
                return objectKlazz.isAssignableFrom(subjectKlazz);
            } catch (ClassNotFoundException nfe) {
                return false;
            }
        }
    }

    private static boolean hasSuper(IndexView index, ClassInfo testSubject, Type testObject) {
        Type superKlazzType = testSubject.superClassType();
        while (superKlazzType != null) {
            if (getName(superKlazzType).equals(getName(testObject))) {
                return true;
            }
            ClassInfo superKlazz = index.getClassByName(getName(superKlazzType));
            if (superKlazz == null) {
                try {
                    Class<?> subjectKlazz = TypeUtil.getClass(testSubject.name().toString());
                    Class<?> objectKlazz = TypeUtil.getClass(testObject);
                    return objectKlazz.isAssignableFrom(subjectKlazz);
                } catch (ClassNotFoundException nfe) {
                    return false;
                }
            }
            superKlazzType = superKlazz.superClassType();
        }
        return false;
    }

    public static DotName getName(Type type) {
        if (type.kind() == Type.Kind.ARRAY) {
            return type.asArrayType().component().name();
        }
        if (type.kind() == Type.Kind.WILDCARD_TYPE) {
            return getBound(type.asWildcardType()).name();
        }
        return type.name();
    }

    public static Type getBound(WildcardType wct) {
        if (wct.extendsBound() != null) {
            return wct.extendsBound();
        } else {
            return OBJECT_TYPE;
        }
    }

    public static Type resolveWildcard(WildcardType wildcardType) {
        return TypeUtil.getBound(wildcardType);
    }

    public static Type resolveWildcard(Type type) {
        if (type.kind() != Type.Kind.WILDCARD_TYPE) {
            return type;
        }
        return TypeUtil.getBound(type.asWildcardType());
    }

    public static boolean equalTypes(Type type1, Type type2) {
        if (type1.name().equals(type2.name())) {
            return true;
        }
        return equalWrappedTypes(type1, type2) || equalWrappedTypes(type2, type1);
    }

    public static boolean equalWrappedTypes(Type primitiveCandidate, Type wrappedCandidate) {
        return primitiveCandidate.kind().equals(Type.Kind.PRIMITIVE) &&
                wrappedCandidate.kind().equals(Type.Kind.CLASS) &&
                isPrimitiveWrapper(primitiveCandidate.asPrimitiveType(), wrappedCandidate);
    }

    public static boolean isPrimitiveWrapper(PrimitiveType primitive, Type wrapped) {
        Class<?> wrapperType;

        switch (primitive.primitive()) {
            case BOOLEAN:
                wrapperType = Boolean.class;
                break;
            case BYTE:
                wrapperType = Byte.class;
                break;
            case CHAR:
                wrapperType = Character.class;
                break;
            case DOUBLE:
                wrapperType = Double.class;
                break;
            case FLOAT:
                wrapperType = Float.class;
                break;
            case INT:
                wrapperType = Integer.class;
                break;
            case LONG:
                wrapperType = Long.class;
                break;
            case SHORT:
                wrapperType = Short.class;
                break;
            default:
                throw new IllegalArgumentException("Unknown primitive: " + primitive);
        }

        return DotName.createSimple(wrapperType.getName()).equals(wrapped.name());
    }

    public static AnnotationInstance getSchemaAnnotation(AnnotationTarget annotationTarget) {
        return getAnnotation(annotationTarget, OpenApiConstants.DOTNAME_SCHEMA);
    }

    public static AnnotationInstance getSchemaAnnotation(ClassInfo field) {
        return getAnnotation(field, OpenApiConstants.DOTNAME_SCHEMA);
    }

    public static AnnotationInstance getSchemaAnnotation(FieldInfo field) {
        return getAnnotation(field, OpenApiConstants.DOTNAME_SCHEMA);
    }

    public static AnnotationInstance getSchemaAnnotation(Type type) {
        return getAnnotation(type, OpenApiConstants.DOTNAME_SCHEMA);
    }

    public static boolean hasAnnotation(AnnotationTarget target, DotName annotationName) {
        switch (target.kind()) {
            case CLASS:
                return target.asClass().classAnnotation(annotationName) != null;
            case FIELD:
                return target.asField().hasAnnotation(annotationName);
            case METHOD:
                return target.asMethod().hasAnnotation(annotationName);
            case METHOD_PARAMETER:
                MethodParameterInfo parameter = target.asMethodParameter();
                return parameter.method()
                        .annotations()
                        .stream()
                        .filter(a -> a.target().kind() == Kind.METHOD_PARAMETER)
                        .filter(a -> a.target().asMethodParameter().position() == parameter.position())
                        .anyMatch(a -> a.name().equals(annotationName));
            case TYPE:
                break;
        }

        return false;
    }

    public static AnnotationInstance getAnnotation(AnnotationTarget annotationTarget, DotName annotationName) {
        if (annotationTarget == null) {
            return null;
        }
        return getAnnotations(annotationTarget).stream()
                .filter(annotation -> annotation.name().equals(annotationName))
                .findFirst()
                .orElse(null);
    }

    /**
     * Convenience method to retrieve the "value" parameter from an annotation bound to the target.
     * The value will be unwrapped from its containing {@link AnnotationValue}.
     *
     * @param <T> the type of the parameter being retrieved
     * @param target the target object annotated with the annotation named by annotationName
     * @param annotationName name of the annotation from which to retrieve the value
     * @return an unwrapped annotation parameter value
     */
    public static <T> T getAnnotationValue(AnnotationTarget target, DotName annotationName) {
        return getAnnotationValue(target, annotationName, OpenApiConstants.PROP_VALUE, null);
    }

    /**
     * Convenience method to retrieve the named parameter from an annotation bound to the target.
     * The value will be unwrapped from its containing {@link AnnotationValue}.
     *
     * @param <T> the type of the parameter being retrieved
     * @param target the target object annotated with the annotation named by annotationName
     * @param annotationName name of the annotation from which to retrieve the value
     * @param propertyName the name of the parameter/property in the annotation
     * @return an unwrapped annotation parameter value
     */
    public static <T> T getAnnotationValue(AnnotationTarget target, DotName annotationName, String propertyName) {
        return getAnnotationValue(target, annotationName, propertyName, null);
    }

    /**
     * Convenience method to retrieve the named parameter from an annotation bound to the target.
     * The value will be unwrapped from its containing {@link AnnotationValue}.
     *
     * @param <T> the type of the parameter being retrieved
     * @param target the target object annotated with the annotation named by annotationName
     * @param annotationName name of the annotation from which to retrieve the value
     * @param propertyName the name of the parameter/property in the annotation
     * @param defaultValue a default value to return if either the annotation or the value are missing
     * @return an unwrapped annotation parameter value
     */
    public static <T> T getAnnotationValue(AnnotationTarget target,
            DotName annotationName,
            String propertyName,
            T defaultValue) {

        AnnotationInstance annotation = getAnnotation(target, annotationName);

        if (annotation != null) {
            return JandexUtil.value(annotation, propertyName);
        }

        return defaultValue;
    }

    public static Collection<AnnotationInstance> getAnnotations(AnnotationTarget type) {
        switch (type.kind()) {
            case CLASS:
                return type.asClass().classAnnotations();
            case FIELD:
                return type.asField().annotations();
            case METHOD:
                return type.asMethod().annotations();
            case METHOD_PARAMETER:
                MethodParameterInfo parameter = type.asMethodParameter();
                return parameter
                        .method()
                        .annotations()
                        .stream()
                        .filter(a -> a.target().kind() == Kind.METHOD_PARAMETER)
                        .filter(a -> a.target().asMethodParameter().position() == parameter.position())
                        .collect(Collectors.toList());
            case TYPE:
                break;
        }
        return Collections.emptyList();
    }

    public static ClassInfo getDeclaringClass(AnnotationTarget type) {
        switch (type.kind()) {
            case FIELD:
                return type.asField().declaringClass();
            case METHOD:
                return type.asMethod().declaringClass();
            case METHOD_PARAMETER:
                MethodParameterInfo parameter = type.asMethodParameter();
                return parameter.method().declaringClass();
            case CLASS:
            case TYPE:
                break;
        }

        return null;
    }

    public static AnnotationInstance getAnnotation(Type type, DotName annotationName) {
        return type.annotations().stream()
                .filter(annotation -> annotation.name().equals(annotationName))
                .findFirst()
                .orElse(null);
    }

    public static AnnotationInstance getAnnotation(ClassInfo field, DotName annotationName) {
        return field.classAnnotations().stream()
                .filter(annotation -> annotation.name().equals(annotationName))
                .findFirst()
                .orElse(null);
    }

    public static AnnotationInstance getAnnotation(FieldInfo field, DotName annotationName) {
        return field.annotations().stream()
                .filter(annotation -> annotation.name().equals(annotationName))
                .findFirst()
                .orElse(null);
    }

    public static final class TypeWithFormat {
        private final SchemaType schemaType;
        private final DataFormat format;

        public TypeWithFormat(@NotNull SchemaType schemaType,
                @NotNull DataFormat format) {
            this.schemaType = schemaType;
            this.format = format;
        }

        public SchemaType getSchemaType() {
            return schemaType;
        }

        public DataFormat getFormat() {
            return format;
        }
    }

    public enum DataFormat {
        NONE(null),
        INT32("int32"),
        INT64("int64"),
        FLOAT("float"),
        DOUBLE("double"),
        BYTE("byte"),
        BINARY("binary"),
        DATE("date"),
        DATE_TIME("date-time"),
        PASSWORD("password");

        private final String format;

        DataFormat(String format) {
            this.format = format;
        }

        public String format() {
            return format;
        }

        public boolean hasFormat() {
            return this != NONE;
        }
    }

}
