/**
 * Spring integration: SpringLarkBatisSession obtains Connections via DataSourceUtils so
 * they participate in Spring-managed transactions — it must never call
 * dataSource.getConnection directly.
 */
package io.github.larkbatis.spring;
