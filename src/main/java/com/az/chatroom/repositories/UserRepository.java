package com.az.chatroom.repositories;

import com.az.generated.jooq.tables.records.AppUserRecord;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.az.generated.jooq.Tables.APP_USER;

@Repository
public class UserRepository {
    private final DSLContext dsl;

    public UserRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public void create(AppUserRecord appUser) {
        dsl.insertInto(APP_USER)
                .set(appUser)
                .execute();
    }

    public boolean delete(UUID uuid) {
        return dsl.deleteFrom(APP_USER)
                .where(APP_USER.ID.eq(uuid))
                .execute() == 1;
    }

    public List<AppUserRecord> fetchUsers(int page, int size) {
        return dsl.selectFrom(APP_USER)
                .orderBy(
                        APP_USER.CREATED_AT.desc(),
                        APP_USER.ID.desc()
                )
                .limit(size + 1)
                .offset(page * size)
                .fetch();
    }

    public Optional<AppUserRecord> findById(UUID uuid) {
        return dsl.selectFrom(APP_USER)
                .where(APP_USER.ID.eq(uuid))
                .fetchOptional();
    }
}
