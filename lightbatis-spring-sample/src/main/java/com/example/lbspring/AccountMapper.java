package com.example.lbspring;

import io.github.lightbatis.annotations.Delete;
import io.github.lightbatis.annotations.Insert;
import io.github.lightbatis.annotations.Mapper;
import io.github.lightbatis.annotations.Options;
import io.github.lightbatis.annotations.Select;
import io.github.lightbatis.annotations.Update;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Stream;

/**
 * An ordinary mapper. Nothing here says "Spring": the generated
 * {@code AccountMapper$$Impl} takes a {@code LightBatisSession} and the
 * generated {@code LightBatisMapperConfiguration} hands it the Spring one.
 */
@Mapper
public interface AccountMapper {

    @Select("SELECT id, owner, balance FROM account WHERE id = #{id}")
    Account findById(long id);

    @Select("SELECT id, owner, balance FROM account ORDER BY id")
    List<Account> findAll();

    @Select("SELECT count(*) FROM account")
    int count();

    /**
     * A cursor under Spring: the stream holds a pooled Connection until it is
     * closed, and {@code release} still goes through {@code DataSourceUtils} —
     * so inside a transaction closing gives nothing back, and outside one it
     * returns the connection to Hikari.
     */
    @Select("SELECT id, owner, balance FROM account ORDER BY id")
    Stream<Account> streamAll();

    @Insert("INSERT INTO account (owner, balance) VALUES (#{owner}, #{balance})")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(Account account);

    @Update("UPDATE account SET balance = #{balance} WHERE id = #{id}")
    int updateBalance(long id, BigDecimal balance);

    @Delete("DELETE FROM account")
    int deleteAll();
}
