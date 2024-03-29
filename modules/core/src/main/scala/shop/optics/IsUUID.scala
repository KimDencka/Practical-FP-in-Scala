package shop.optics

import monocle.Iso
import shop.ext.derevo.Derive

import java.util.UUID

trait IsUUID[A] {
  def _UUID: Iso[UUID, A]
}

object IsUUID {
  def apply[A: IsUUID]: IsUUID[A] = implicitly

  implicit val identityUUID: IsUUID[UUID] = new IsUUID[UUID] {
    val _UUID: Iso[UUID, UUID] = Iso[UUID, UUID](identity)(identity)
  }
}

object uuid extends Derive[IsUUID]
