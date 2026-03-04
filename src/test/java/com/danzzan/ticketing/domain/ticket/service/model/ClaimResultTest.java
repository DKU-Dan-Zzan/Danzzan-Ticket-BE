package com.danzzan.ticketing.domain.ticket.service.model;

import com.danzzan.ticketing.domain.ticket.redis.TicketRequestStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ClaimResultTest {

    @Test
    void successUsesRemaining() {
        ClaimResult result = ClaimResult.success(42L);

        assertThat(result.status()).isEqualTo(TicketRequestStatus.SUCCESS);
        assertThat(result.remaining()).isEqualTo(42L);
    }

    @Test
    void soldOutAndAlreadyUseNullRemaining() {
        ClaimResult soldOut = ClaimResult.soldOut();
        ClaimResult already = ClaimResult.already();

        assertThat(soldOut.status()).isEqualTo(TicketRequestStatus.SOLD_OUT);
        assertThat(soldOut.remaining()).isNull();
        assertThat(already.status()).isEqualTo(TicketRequestStatus.ALREADY);
        assertThat(already.remaining()).isNull();
    }

    @Test
    void constructorRejectsInvalidRemainingForStatus() {
        assertThatThrownBy(() -> new ClaimResult(TicketRequestStatus.SUCCESS, null))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> new ClaimResult(TicketRequestStatus.SOLD_OUT, 1L))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> new ClaimResult(TicketRequestStatus.ALREADY, 1L))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
