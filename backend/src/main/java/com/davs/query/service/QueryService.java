package com.davs.query.service;

import com.davs.datasource.dto.FieldDto;
import com.davs.query.dto.QueryResult;
import com.davs.query.dto.QuerySpec;
import com.davs.viz.domain.ChartType;
import java.util.List;

public interface QueryService {

    QueryResult executeQuery(QuerySpec querySpec);

    ChartType inferChartType(List<FieldDto> columns, List<FieldDto> rows);
}
