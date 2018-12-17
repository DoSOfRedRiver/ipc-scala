package ipc.services

import java.util.concurrent.atomic.AtomicReference

import com.google.protobuf.empty.Empty
import models.Test.{RetrieveServiceGrpc, User, UserProfile}

import scala.concurrent.Future

class SimpleRetrieve extends RetrieveServiceGrpc.RetrieveService {
  var lastExpr = new AtomicReference[UserProfile](null)

  override def getUserInfo(request: User): Future[UserProfile] = {
    val res = request.name match {
      case "Donald" => UserProfile(1, request.name, "Trump")
      case "John"   => UserProfile(2, request.name, "Doe")
      case "Kevin"  => UserProfile(3, request.name, "Smith")
      case "Bernie" => UserProfile(4, request.name, "sanders")
    }

    lastExpr.set(res)
    Future.successful(res)
  }

  override def lastUserInfo(request: Empty): Future[UserProfile] = Future.successful(lastExpr.get())
}
