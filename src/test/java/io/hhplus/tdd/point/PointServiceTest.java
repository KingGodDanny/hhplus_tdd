package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import java.beans.Transient;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
public class PointServiceTest {

    // 가짜 객체 , 보통 dao나 repository
    @Mock
    PointHistoryTable pointHistoryTable;

    @Mock
    UserPointTable userPointTable;

    @InjectMocks // @Mock으로 만들어진 인스턴스들을 자동으로 주입해준다. 보통 서비스
    PointService pointService;

//    @BeforeEach // 각각의 테스트 케이스 시작 전에 매번 실행 vs @BeforeAll -> 전체 테스트 시작하기 전 한번만 실행 (static 사용)
//    void setUp() {
        // @Mock와 @InjectMocks 애노테이션이 붙은 필드를 자동으로 인식하여 Mockito가 목 객체를 초기화하고 필드에 할당하는 데 사용된다.
        // 또한 openMocks()는 목 객체 초기화 중에 발생하는 오류를 발견하기 쉽게 한다.
        // @ExtendWith(MockitoExtension.class)를 사용하면 따로 아래를 호출하지 않아도 자동으로 Mock 객체들을 초기화 해준다.
//        MockitoAnnotations.openMocks(this);
//    }

    private static final long USER_ID = 1L;

    @Test
    @DisplayName("유저 포인트 조회")
    void point() {

        //given :
        UserPoint userPoint = new UserPoint(USER_ID, 10, System.currentTimeMillis());
        //Mockito  <-> when ~ thenReturn
        given(userPointTable.selectById(USER_ID)).willReturn(userPoint);

        //when
        UserPoint result = pointService.selectById(USER_ID);

        //then
        assertThat(result.point()).isEqualTo(userPoint.point());

    }

    @Test
    @DisplayName("유저 포인트 충전 성공")
    void chargeSuccess() {

        //given
        long id = 1L;
        long currentPoint = 5_000L;
        long amount = 1_000L;

        UserPoint before = new UserPoint(id, currentPoint, System.currentTimeMillis());
        UserPoint after = new UserPoint(id, currentPoint + amount, System.currentTimeMillis());

        given(userPointTable.selectById(id)).willReturn(before);
        given(userPointTable.insertOrUpdate(id, currentPoint + amount)).willReturn(after);

        //when
        UserPoint resultPoint = pointService.charge(id, amount);

        //then
        assertThat(resultPoint.point()).isEqualTo(currentPoint + amount);

    }

    @Test
    @DisplayName("최소 충전 포인트는 1,000 입니다. - 실패 케이스")
    void minimumChargePoint() {
        //given
        long amount = 900L;

        //when & then
       assertThrows(IllegalArgumentException.class, () -> {
            pointService.charge(USER_ID, amount);
        });
    }

    @Test
    @DisplayName("충전 후 최대 보유 포인트는 10,000 입니다. - 실패 케이스")
    void maximumHavePoint() {
        //given
        long amount = 9_000L;
        UserPoint userPoint = new UserPoint(USER_ID, 2000, System.currentTimeMillis());
        given(userPointTable.selectById(USER_ID)).willReturn(userPoint); // userPoint 반환하도록 목 설정

        //when & then
        assertThrows(IllegalArgumentException.class, ()-> {
           pointService.charge(userPoint.id(), amount);
        });
    }

    @Test
    @DisplayName("유저 포인트 사용 성공")
    void usePointSuccess() {
        //given
        long currentPoint = 5_000L;
        long usePoint = 1_000L;

        UserPoint before = new UserPoint(USER_ID, currentPoint, System.currentTimeMillis());
        UserPoint after = new UserPoint(USER_ID, currentPoint - usePoint, System.currentTimeMillis());

        given(userPointTable.selectById(USER_ID)).willReturn(before);
        given(userPointTable.insertOrUpdate(USER_ID, currentPoint - usePoint)).willReturn(after);

        //when
        UserPoint resultPoint = pointService.use(USER_ID, usePoint);

        //then
        assertThat(resultPoint.point()).isEqualTo(currentPoint - usePoint);
    }

    @Test
    @DisplayName("최소 사용 포인트는 1,000 입니다. - 실패 케이스")
    void minimumUsePoint() {
        //given
        long amount = 900L;

        //when & then
        assertThrows(IllegalArgumentException.class, () -> {
            pointService.use(USER_ID, amount);
        });
    }
    
    @Test
    @DisplayName("현재 갖고 있는 포인트보다 사용 포인트가 높으면 안됩니다.(잔고부족) - 실패 케이스")
    void usePointCheck() {
        //given
        long usePoint = 3_000L;
        UserPoint userPoint = new UserPoint(USER_ID, 2000, System.currentTimeMillis());
        given(userPointTable.selectById(USER_ID)).willReturn(userPoint); // userPoint 반환하도록 목 설정

        //when & then
        assertThrows(IllegalArgumentException.class, ()-> {
            pointService.use(userPoint.id(), usePoint);
        });
    }
    
    @Test
    @DisplayName("유저의 포인트 사용 내역 조회")
    void userPointHistory() {
        //given
        List<PointHistory> pointHistory = List.of(
                new PointHistory(1L, USER_ID, 3000, TransactionType.CHARGE, System.currentTimeMillis()),
                new PointHistory(2L, USER_ID, 2000, TransactionType.CHARGE, System.currentTimeMillis()),
                new PointHistory(5L, USER_ID, 4000, TransactionType.CHARGE, System.currentTimeMillis())
        );
        given(pointHistoryTable.selectAllByUserId(USER_ID)).willReturn(pointHistory);

        //when
        List<PointHistory> histories = pointService.selectAllByUserId(USER_ID);

        assertThat(histories).isEqualTo(pointHistory);
        assertThat(histories.size()).isEqualTo((pointHistory.size()));
    }
    
}
