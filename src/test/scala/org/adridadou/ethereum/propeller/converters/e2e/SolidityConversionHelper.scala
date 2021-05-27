package org.adridadou.ethereum.propeller.converters.e2e

import org.adridadou.ethereum.propeller.backend.{EthereumTest, TestConfig}

import java.io.File
import java.security.Security
import org.adridadou.ethereum.propeller.keystore.AccountProvider
import org.adridadou.ethereum.propeller.solidity.SolidityContractDetails
import org.adridadou.ethereum.propeller.values.EthValue.ether
import org.adridadou.ethereum.propeller.values.{
  EthAccount,
  EthAddress,
  SoliditySourceFile
}
import org.adridadou.ethereum.propeller.{
  CoreEthereumFacadeProvider,
  EthereumConfig,
  EthereumFacade
}
import org.bouncycastle.jce.provider.BouncyCastleProvider

import scala.reflect.ClassTag
import scala.util.{Failure, Success, Try}

/**
  * Created by davidroon on 13.04.17.
  * This code is released under Apache 2 license
  */

object SolidityConversionHelper {
  Security.addProvider(new BouncyCastleProvider())
  val mainAccount: EthAccount = AccountProvider.fromPrivateKey(
    "19fcc1bd936e0cc8d2812cc574875b5fd0d6c6288e56b523e4023529c68ef6d4"
  )

  val facade: EthereumFacade = CoreEthereumFacadeProvider
    .create(
      new EthereumTest(
        TestConfig.builder.balance(mainAccount, ether(1000)).build
      ),
      EthereumConfig.builder().build()
    )

  val contract: SolidityContractDetails = facade
    .compile(
      SoliditySourceFile
        .from(new File(getClass.getResource("/conversionContract.sol").getFile))
    )
    .findContract("myContract")
    .get()

  val contractAddress: EthAddress =
    facade.publishContract(contract, mainAccount).get()
}

trait SolidityConversionHelper {

  def contractObject[T]()(implicit tag: ClassTag[T]): T =
    Try(
      SolidityConversionHelper.facade
        .createContractProxy(
          SolidityConversionHelper.contract,
          SolidityConversionHelper.contractAddress,
          SolidityConversionHelper.mainAccount,
          tag.runtimeClass
        )
        .asInstanceOf[T]
    ) match {
      case Success(result) =>
        result
      case Failure(ex) =>
        throw ex
    }

  def contractObjectWithAddress[T]()(implicit
      tag: ClassTag[T]
  ): (EthAddress, T) = {
    (
      SolidityConversionHelper.contractAddress,
      SolidityConversionHelper.facade
        .createContractProxy(
          SolidityConversionHelper.contract,
          SolidityConversionHelper.contractAddress,
          SolidityConversionHelper.mainAccount,
          tag.runtimeClass
        )
        .asInstanceOf[T]
    )
  }
}
