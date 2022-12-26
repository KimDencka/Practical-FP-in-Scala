package shop.sql

import shop.domain.auth.AuthPayload._
import shop.domain.brand.BrandPayload._
import shop.domain.category.CategoryPayload._
import shop.domain.order.OrderPayload._
import shop.domain.item.ItemPayload._
import skunk.Codec
import skunk.codec.all._
import squants.market.{ Money, USD }

object codecs {
  val brandId: Codec[BrandId]     = uuid.imap[BrandId](v => BrandId(v))(_.value)
  val brandName: Codec[BrandName] = varchar.imap[BrandName](v => BrandName(v))(_.value)

  val categoryId: Codec[CategoryId]     = uuid.imap[CategoryId](v => CategoryId(v))(_.value)
  val categoryName: Codec[CategoryName] = varchar.imap[CategoryName](v => CategoryName(v))(_.value)

  val itemId: Codec[ItemId]            = uuid.imap[ItemId](v => ItemId(v))(_.value)
  val itemName: Codec[ItemName]        = varchar.imap[ItemName](v => ItemName(v))(_.value)
  val itemDesc: Codec[ItemDescription] = varchar.imap[ItemDescription](v => ItemDescription(v))(_.value)

  val orderId: Codec[OrderId]     = uuid.imap[OrderId](v => OrderId(v))(_.value)
  val paymentId: Codec[PaymentId] = uuid.imap[PaymentId](v => PaymentId(v))(_.value)

  val userId: Codec[UserId]     = uuid.imap[UserId](v => UserId(v))(_.value)
  val userName: Codec[UserName] = varchar.imap[UserName](v => UserName(v))(_.value)

  val money: Codec[Money] = numeric.imap[Money](USD(_))(_.amount)

  val encPassword: Codec[EncryptedPassword] = varchar.imap[EncryptedPassword](v => EncryptedPassword(v))(_.value)

}
