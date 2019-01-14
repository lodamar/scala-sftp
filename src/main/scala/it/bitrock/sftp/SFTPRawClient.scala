package it.bitrock.sftp

import java.util.Properties
import scala.collection.JavaConverters._

import com.jcraft.jsch.{ ChannelSftp, JSch }

trait SFTPRawClient {
  def get(from: String, to: String)
  def put(from: String, to: String)
  def rm(from: String)
  def ls(of: String): List[String]
}

class SFTPRawClientJsch(host: String, user: String, password: String) extends SFTPRawClient {
  private lazy val client: ChannelSftp = {
    val jsch   = new JSch()
    val config = new Properties
    config.put("StrictHostKeyChecking", "no")

    val session = jsch.getSession(user, host)
    session.setConfig(config)
    session.setPassword(password)
    session.connect()

    val sftpChannel = session.openChannel("sftp").asInstanceOf[ChannelSftp]
    sftpChannel.connect()

    sftpChannel
  }

  override def get(from: String, to: String): Unit = client.get(from, to)

  override def put(from: String, to: String): Unit = client.put(from, to)

  override def rm(from: String): Unit = client.rm(from)

  override def ls(of: String): List[String] =
    client.ls(of).asInstanceOf[java.util.Vector[ChannelSftp#LsEntry]].asScala.toList.map(_.getFilename)
}
