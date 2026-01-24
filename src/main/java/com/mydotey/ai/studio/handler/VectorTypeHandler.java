package com.mydotey.ai.studio.handler;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;

import java.sql.*;
import java.util.Arrays;

/**
 * Type handler for PostgreSQL vector type
 * Converts between Java float[] and PostgreSQL vector
 *
 * PostgreSQL pgvector extension uses the format: '[1.0,2.0,3.0]'
 */
@MappedTypes({float[].class})
public class VectorTypeHandler extends BaseTypeHandler<float[]> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, float[] parameter, JdbcType jdbcType) throws SQLException {
        // Convert float array to PostgreSQL vector format: "[1.0,2.0,3.0]"
        String vectorString = formatVector(parameter);
        // Use setObject with Types.OTHER for PostgreSQL custom types
        ps.setObject(i, vectorString, Types.OTHER);
    }

    @Override
    public float[] getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return parseVector(rs.getString(columnName));
    }

    @Override
    public float[] getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return parseVector(rs.getString(columnIndex));
    }

    @Override
    public float[] getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return parseVector(cs.getString(columnIndex));
    }

    /**
     * Format float array to PostgreSQL vector string: "[1.0,2.0,3.0]"
     */
    private String formatVector(float[] vector) {
        if (vector == null || vector.length == 0) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(vector[i]);
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * Parse vector string from PostgreSQL to float array
     * Input format: "[1.0,2.0,3.0]"
     */
    private float[] parseVector(String vectorString) {
        if (vectorString == null || vectorString.trim().isEmpty()) {
            return null;
        }

        String trimmed = vectorString.trim();

        // Remove brackets and split by comma
        if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
            trimmed = trimmed.substring(1, trimmed.length() - 1);
        }

        if (trimmed.trim().isEmpty()) {
            return new float[0];
        }

        String[] parts = trimmed.split(",");
        float[] result = new float[parts.length];
        for (int i = 0; i < parts.length; i++) {
            result[i] = Float.parseFloat(parts[i].trim());
        }

        return result;
    }
}
