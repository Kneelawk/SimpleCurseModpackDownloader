package org.kneelawk.simplecursemodpackdownloader.net

/*
 * NetworkClient hierarchy.
 * 
 * How much functionality should be defined here?
 * 
 * It would make more sense for the actual download tasks to be hierarchical.
 * So you can have a DownloadTask that is a child of a ModDownloadTask that is a child of a ModpackDownloadTask.
 * 
 * Client types:
 * DownloadClient, RestClient?, StringClient?
 */
trait NetworkClient {
}