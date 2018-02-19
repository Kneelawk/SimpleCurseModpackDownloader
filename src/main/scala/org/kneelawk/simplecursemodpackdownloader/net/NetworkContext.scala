package org.kneelawk.simplecursemodpackdownloader.net

import java.io.Closeable

import org.apache.http.impl.nio.client.CloseableHttpAsyncClient
import org.apache.http.impl.nio.client.HttpAsyncClients
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder

class NetworkContext(c: CloseableHttpAsyncClient) extends Closeable {
  def client = c

  def start() = c.start()

  def close() = c.close()
}

object NetworkContext {
  def apply(): NetworkContext = new NetworkContext(HttpAsyncClients.createDefault())

  def apply(client: CloseableHttpAsyncClient): NetworkContext = new NetworkContext(client)

  def custom(): NetworkContextBuilder = new NetworkContextBuilder(HttpAsyncClients.custom())
}

class NetworkContextBuilder(b: HttpAsyncClientBuilder) {
  def setUserAgent(userAgent: String): this.type = { b.setUserAgent(userAgent); this }

  def build(): NetworkContext = new NetworkContext(b.build())
}