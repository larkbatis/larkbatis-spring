/**
 * Spring integration: SpringLightBatisSession obtains Connections via DataSourceUtils so
 * they participate in Spring-managed transactions — it must never call
 * dataSource.getConnection() directly (design §10).
 */
package io.github.lightbatis.spring;
