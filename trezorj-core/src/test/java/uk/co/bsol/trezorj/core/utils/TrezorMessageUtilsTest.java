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

    byte[] expectedPrevHash = new byte[] {95, 107, -44, 58, 92, 38, -71, -24, -48, 55, -60, 5, -5, -41, 5, -49, 75, -105, -60, 54, 82, -123, -115, 88, 36, -99, 19, -43, 39, -23, -7, 102};
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
