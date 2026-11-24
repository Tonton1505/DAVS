package vn.edu.davs.backend.specification;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.data.jpa.domain.Specification;

/**
 * Builder for composing {@link GenericSpecification} instances from search query strings.
 */
public class GenericSpecificationsBuilder<T> {

    private static final Pattern SEARCH_PATTERN =
            Pattern.compile("([a-zA-Z0-9_.]+?)(>=|<=|!=|!|:|>|<)([a-zA-Z0-9_.@\\s\\p{L}-]+?)(?:,|$)");

    private final List<SearchCriteria> params = new ArrayList<>();

    public GenericSpecificationsBuilder<T> with(String key, String operation, Object value) {
        params.add(new SearchCriteria(key, operation, value));
        return this;
    }

    public GenericSpecificationsBuilder<T> with(SearchCriteria criteria) {
        params.add(criteria);
        return this;
    }

    public GenericSpecificationsBuilder<T> withSearchQuery(String search) {
        if (search == null || search.trim().isEmpty()) {
            return this;
        }
        Matcher matcher = SEARCH_PATTERN.matcher(search);
        while (matcher.find()) {
            with(matcher.group(1).trim(), matcher.group(2).trim(), matcher.group(3).trim());
        }
        return this;
    }

    public Specification<T> build() {
        if (params.isEmpty()) {
            return null;
        }
        Specification<T> result = new GenericSpecification<>(params.get(0));
        for (int i = 1; i < params.size(); i++) {
            result = Specification.where(result).and(new GenericSpecification<>(params.get(i)));
        }
        return result;
    }
}
