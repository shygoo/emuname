#include "XGamepads.h"
#include "net_shygoo_jxgamepads_JXGamepads.h"


JNIEXPORT jboolean JNICALL Java_net_shygoo_jxgamepads_JXGamepads_xgIsConnected(JNIEnv * jenv, jclass cl, jint gamepadNum){
	return XGamepads::isConnected(gamepadNum);
}
  
JNIEXPORT jdoubleArray JNICALL Java_net_shygoo_jxgamepads_JXGamepads_xgGetAxes(JNIEnv * jenv, jclass cl, jint gamepadNum){
	jdoubleArray jaxes = jenv->NewDoubleArray(MAX_AXES);
	double* axes = XGamepads::getAxes(gamepadNum);
	jenv->SetDoubleArrayRegion(jaxes, 0, MAX_AXES, (jdouble*)axes);
	return jaxes;
}
  
JNIEXPORT jbyteArray JNICALL Java_net_shygoo_jxgamepads_JXGamepads_xgGetButtons(JNIEnv * jenv, jclass cl, jint gamepadNum){
	jbyteArray jbuttons = jenv->NewByteArray(MAX_BUTTONS);
	BYTE* buttons = XGamepads::getButtons(gamepadNum);
	jenv->SetByteArrayRegion(jbuttons, 0, MAX_BUTTONS, (jbyte*)buttons);
	return jbuttons;
}
  
JNIEXPORT void JNICALL Java_net_shygoo_jxgamepads_JXGamepads_xgSetVibration(JNIEnv * jenv, jclass cl, jint gamepadNum, jdouble left, jdouble right){
	XGamepads::setVibration(gamepadNum, left, right);
}
