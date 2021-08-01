package com.mshernandez.coinaccount.service.result;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

@Accessors(chain = true)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class WithdrawRequestResult
{
    private String txid;
    private long withdrawAmount;
    private long feeAmount;
    private long totalCost;
}