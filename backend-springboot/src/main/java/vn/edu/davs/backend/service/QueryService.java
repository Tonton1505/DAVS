package vn.edu.davs.backend.service;

import vn.edu.davs.backend.dto.QueryResult;
import vn.edu.davs.backend.dto.QuerySpec;
import vn.edu.davs.backend.enu.ChartType;
import vn.edu.davs.backend.model.DavsUser;

/**
 * Query Engine and Chart Type Inference Service (UC15, T-101 RLS & T-103 Query Cache).
 */
public interface QueryService {


    public static final int MARKS_WARNING_THRESHOLD = 100_000;


    QueryResult executeQuery(QuerySpec spec);


    QueryResult executeQuery(QuerySpec spec, DavsUser actor);


    ChartType inferChartType(QuerySpec spec);
}
