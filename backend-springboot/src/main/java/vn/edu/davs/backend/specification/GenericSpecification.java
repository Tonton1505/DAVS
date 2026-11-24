package vn.edu.davs.backend.specification;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

/**
 * Generic JPA {@link Specification} supporting basic comparison and pattern operations.
 */
public class GenericSpecification<T> implements Specification<T> {

    private final SearchCriteria criteria;

    public GenericSpecification(SearchCriteria criteria) {
        this.criteria = criteria;
    }

    public SearchCriteria getCriteria() {
        return criteria;
    }

    @Override
    public Predicate toPredicate(Root<T> root, CriteriaQuery<?> query, CriteriaBuilder builder) {
        if (criteria == null || criteria.getKey() == null) {
            return null;
        }
        Path<?> path;
        try {
            path = root.get(criteria.getKey());
        } catch (IllegalArgumentException e) {
            return null;
        }
        Object value = criteria.getValue();
        if (value == null) {
            return null;
        }
        String operation = criteria.getOperation() != null ? criteria.getOperation() : ":";
        String strValue = value.toString();
        Class<?> javaType = path.getJavaType();

        return switch (operation) {
            case ">" -> Number.class.isAssignableFrom(javaType)
                    ? builder.gt(root.get(criteria.getKey()), parseNumber(javaType, strValue))
                    : builder.greaterThan(root.get(criteria.getKey()), strValue);
            case ">=" -> Number.class.isAssignableFrom(javaType)
                    ? builder.ge(root.get(criteria.getKey()), parseNumber(javaType, strValue))
                    : builder.greaterThanOrEqualTo(root.get(criteria.getKey()), strValue);
            case "<" -> Number.class.isAssignableFrom(javaType)
                    ? builder.lt(root.get(criteria.getKey()), parseNumber(javaType, strValue))
                    : builder.lessThan(root.get(criteria.getKey()), strValue);
            case "<=" -> Number.class.isAssignableFrom(javaType)
                    ? builder.le(root.get(criteria.getKey()), parseNumber(javaType, strValue))
                    : builder.lessThanOrEqualTo(root.get(criteria.getKey()), strValue);
            case "!", "!=" -> javaType == String.class
                    ? builder.notLike(builder.lower(root.get(criteria.getKey())), "%" + strValue.toLowerCase() + "%")
                    : builder.notEqual(path, castToRequiredType(javaType, strValue));
            default -> javaType == String.class
                    ? builder.like(builder.lower(root.get(criteria.getKey())), "%" + strValue.toLowerCase() + "%")
                    : builder.equal(path, castToRequiredType(javaType, strValue));
        };
    }

    private Number parseNumber(Class<?> javaType, String strValue) {
        if (javaType == Long.class || javaType == long.class) {
            return Long.parseLong(strValue);
        }
        if (javaType == Double.class || javaType == double.class) {
            return Double.parseDouble(strValue);
        }
        return Integer.parseInt(strValue);
    }

    private Object castToRequiredType(Class<?> fieldType, String value) {
        if (fieldType == UUID.class) {
            return UUID.fromString(value);
        }
        if (fieldType == Boolean.class || fieldType == boolean.class) {
            return Boolean.parseBoolean(value);
        }
        if (Number.class.isAssignableFrom(fieldType) || fieldType.isPrimitive()) {
            return parseNumber(fieldType, value);
        }
        return value;
    }
}
