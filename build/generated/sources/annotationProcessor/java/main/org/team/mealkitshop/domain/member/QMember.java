package org.team.mealkitshop.domain.member;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QMember is a Querydsl query type for Member
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QMember extends EntityPathBase<Member> {

    private static final long serialVersionUID = 1720475120L;

    public static final QMember member = new QMember("member1");

    public final org.team.mealkitshop.common.QBaseTimeEntity _super = new org.team.mealkitshop.common.QBaseTimeEntity(this);

    public final ListPath<org.team.mealkitshop.domain.board.ReviewBoard, org.team.mealkitshop.domain.board.QReviewBoard> boards = this.<org.team.mealkitshop.domain.board.ReviewBoard, org.team.mealkitshop.domain.board.QReviewBoard>createList("boards", org.team.mealkitshop.domain.board.ReviewBoard.class, org.team.mealkitshop.domain.board.QReviewBoard.class, PathInits.DIRECT2);

    public final StringPath email = createString("email");

    public final EnumPath<org.team.mealkitshop.common.Grade> grade = createEnum("grade", org.team.mealkitshop.common.Grade.class);

    public final BooleanPath marketingYn = createBoolean("marketingYn");

    public final StringPath memberName = createString("memberName");

    public final NumberPath<Long> mno = createNumber("mno", Long.class);

    public final StringPath password = createString("password");

    public final StringPath phone = createString("phone");

    public final NumberPath<Integer> points = createNumber("points", Integer.class);

    public final EnumPath<org.team.mealkitshop.common.Provider> provider = createEnum("provider", org.team.mealkitshop.common.Provider.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> regTime = _super.regTime;

    public final EnumPath<org.team.mealkitshop.common.Role> role = createEnum("role", org.team.mealkitshop.common.Role.class);

    public final EnumPath<org.team.mealkitshop.common.Status> status = createEnum("status", org.team.mealkitshop.common.Status.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updateTime = _super.updateTime;

    public QMember(String variable) {
        super(Member.class, forVariable(variable));
    }

    public QMember(Path<? extends Member> path) {
        super(path.getType(), path.getMetadata());
    }

    public QMember(PathMetadata metadata) {
        super(Member.class, metadata);
    }

}

