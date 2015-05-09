function WallpostVk() {
	// Does nothing
};
WallpostVk.prototype.init = function(appId, successCallback, errorCallback) {
	cordova.exec(successCallback, errorCallback, "WallpostVk", "initWallpostVk", [appId]);
};

WallpostVk.prototype.login = function(successCallback, errorCallback) {
	cordova.exec(successCallback, errorCallback, "WallpostVk", "login");
};

WallpostVk.prototype.share = function(sourceURL, description, imageURL, successCallback, errorCallback) {
	cordova.exec(successCallback, errorCallback, "WallpostVk", "share", [sourceURL, description, imageURL]);
};

WallpostVk.prototype.getFriends = function(successCallback, errorCallback) {
	cordova.exec(successCallback, errorCallback, "WallpostVk", "getFriends",[]);
};

WallpostVk.prototype.postWall = function(friendID, sourceURL, description, successCallback, errorCallback) {
	cordova.exec(successCallback, errorCallback, "WallpostVk", "postWall",[friendID, sourceURL, description]);
};

WallpostVk.prototype.logout = function(successCallback, errorCallback) {
	cordova.exec(successCallback, errorCallback, "WallpostVk", "logout");
};

module.exports = new WallpostVk();

