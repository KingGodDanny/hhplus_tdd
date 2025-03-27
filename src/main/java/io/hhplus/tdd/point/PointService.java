package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.apache.catalina.User;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PointService {

    private final PointHistoryTable pointHistoryTable;
    private final UserPointTable userPointTable;

    private static final long MAX_POINT = 10_000L;           // 최대 보유 포인트
    private static final long MIN_CHARGE_AMOUNT = 1_000L;    // 최소 충전 포인트
    private static final long MIN_USE_AMOUNT = 1_000L;       // 최소 사용 포인트

    /**
    유저의 포인트 조회를 조회합니다.
    */
    public UserPoint selectById(long id) {
        return userPointTable.selectById(id);
    }

    /*
    유저의 포인트를 충전합니다.
    1) 최소 충전 포인트는 1,000 입니다.
    2) 최대 보유 포인트는 10,000 입니다.
     * */
    public UserPoint charge(long id, long amount) {

        if(amount < MIN_CHARGE_AMOUNT) {
            throw new IllegalArgumentException("최소 충전 포인트는 1,000 입니다.");
        }

        UserPoint current = userPointTable.selectById(id);
        long afterChargePoint = current.point() + amount;

        if(afterChargePoint > MAX_POINT) {
            throw new IllegalArgumentException("최대 보유 포인트는 10,000 입니다.");
        }


        UserPoint updateUserPoint = userPointTable.insertOrUpdate(id, afterChargePoint);
        pointHistoryTable.insert(id, afterChargePoint, TransactionType.CHARGE, System.currentTimeMillis());

        return updateUserPoint;
    }

    /*
    유저의 포인트를 사용합니다.
    1) 최소 사용 포인트는 1,000 입니다.
    2) 현재 갖고 있는 포인트보다 사용 포인트가 높으면 안됩니다.(잔고부족)
    * */
    public UserPoint use(long id, long amount) {

        if(amount < MIN_USE_AMOUNT) {
            throw new IllegalArgumentException("최소 사용 포인트는 1,000 입니다.");
        }

        UserPoint current = userPointTable.selectById(id);

        if(current.point() < amount) {
            throw new IllegalArgumentException("포인트가 부족합니다.");
        }

        long afterUsePoint = current.point() - amount;

        UserPoint usePoint = userPointTable.insertOrUpdate(id, afterUsePoint);
        pointHistoryTable.insert(id, afterUsePoint, TransactionType.USE, System.currentTimeMillis());

        return usePoint;
    }

    // 유저의 포인트 충전/이용 내역을 조회
    public List<PointHistory> selectAllByUserId(long id) {
        return pointHistoryTable.selectAllByUserId(id);
    }

}
