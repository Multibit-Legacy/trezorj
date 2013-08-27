package uk.co.bsol.trezorj.core.utils;

import com.google.bitcoin.core.Address;
import com.google.bitcoin.core.Transaction;
import org.junit.Test;
import uk.co.bsol.trezorj.core.protobuf.TrezorMessage;

import java.math.BigInteger;

import static org.fest.assertions.api.Assertions.assertThat;


public class TrezorMessageUtilsTest {

  @Test
  public void testNewTxInput() throws Exception {

    // Arrange
    Address ourReceivingAddress = FakeTransactions.getElectrumAddressN(new int[] {0,0});
    Address ourChangeAddress = FakeTransactions.getElectrumAddressN(new int[] {0,1});
    Address random2Address = FakeTransactions.asMainNetAddress("1MKw8vWxvBnaBcrL2yXvZceqyRMoeG2kRn");

    // Build a fake transaction
    Transaction tx = FakeTransactions.newMainNetFakeTx(
      ourReceivingAddress,
      ourChangeAddress,
      random2Address,
      BigInteger.TEN,
      BigInteger.ONE
    );

    byte[] expectedPrevHash = new byte[] {-37, 34, -99, 85, -58, 79, 11, -8, 14, 64, 35, -18, 54, 14, -46, -43, -91, -66, 98, -68, 117, 99, 67, 49, -63, 86, -77, -75, -115, -122, 69, -43};
    byte[] expectedScriptSig = new byte[] {};

    // Act
    TrezorMessage.TxInput txInput = TrezorMessageUtils.newTxInput(tx, 0);

    // Assert
    assertThat(txInput.getAmount()).isEqualTo(BigInteger.TEN.longValue());

    assertThat(txInput.getPrevHash().toByteArray()).isEqualTo(expectedPrevHash);
    assertThat(txInput.getPrevIndex()).isEqualTo(0);
    assertThat(txInput.getScriptSig().toByteArray()).isEqualTo(expectedScriptSig);

  }

  @Test
  public void testNewTxOutput() throws Exception {

    // Arrange
    Address ourReceivingAddress = FakeTransactions.getElectrumAddressN(new int[] {0,0});
    Address ourChangeAddress = FakeTransactions.getElectrumAddressN(new int[] {0,1});
    Address random2Address = FakeTransactions.asMainNetAddress("1MKw8vWxvBnaBcrL2yXvZceqyRMoeG2kRn");

    Transaction tx = FakeTransactions.newMainNetFakeTx(
      ourReceivingAddress,
      ourChangeAddress,
      random2Address,
      BigInteger.TEN,
      BigInteger.ONE
    );

    // Act
    TrezorMessage.TxOutput txOutput0 = TrezorMessageUtils.newTxOutput(tx, 0); // Payment
    TrezorMessage.TxOutput txOutput1 = TrezorMessageUtils.newTxOutput(tx, 1); // Change

    // Assert
    assertThat(txOutput0.getAmount()).isEqualTo(1L); // Payment
    assertThat(txOutput1.getAmount()).isEqualTo(9L); // Change

    assertThat(txOutput0.getAddress()).isEqualTo(random2Address.toString());
    assertThat(txOutput1.getAddress()).isEqualTo(ourChangeAddress.toString());

    assertThat(txOutput0.getIndex()).isEqualTo(0);
    assertThat(txOutput1.getIndex()).isEqualTo(1);

  }


}
