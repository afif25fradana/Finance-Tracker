package android.net

import android.os.Parcel

class TestUri(private val uriString: String = "content://test/dummy") : Uri() {
  override fun isHierarchical(): Boolean = true
  override fun isRelative(): Boolean = false
  override fun getScheme(): String = "content"
  override fun getSchemeSpecificPart(): String = "//test/dummy"
  override fun getEncodedSchemeSpecificPart(): String = "//test/dummy"
  override fun getAuthority(): String = "test"
  override fun getEncodedAuthority(): String = "test"
  override fun getUserInfo(): String? = null
  override fun getEncodedUserInfo(): String? = null
  override fun getHost(): String = "test"
  override fun getPort(): Int = -1
  override fun getPath(): String = "/dummy"
  override fun getEncodedPath(): String = "/dummy"
  override fun getQuery(): String? = null
  override fun getEncodedQuery(): String? = null
  override fun getFragment(): String? = null
  override fun getEncodedFragment(): String? = null
  override fun getPathSegments(): List<String> = listOf("dummy")
  override fun getLastPathSegment(): String = "dummy"
  override fun buildUpon(): Builder? = null
  override fun toString(): String = uriString
  override fun describeContents(): Int = 0
  override fun writeToParcel(dest: Parcel, flags: Int) {}
  override fun compareTo(other: Uri): Int = uriString.compareTo(other.toString())
}
