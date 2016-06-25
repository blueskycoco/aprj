#include <stdio.h>  
#include <stdlib.h>
#include <termios.h>
#include <unistd.h> 
#include <sys/types.h>
#include <sys/stat.h> 
#include <fcntl.h>
#include <string.h>
#include <assert.h>
#include <sys/socket.h>
#include <net/if.h>
//#include <cutils/properties.h>
#include <sys/wait.h>
#include <android/log.h>
#include <stdint.h>
#include <getopt.h>
#include <sys/ioctl.h>
#include <linux/types.h>
//#include <linux/spi/spidev.h>
//#include <linux/i2c-dev.h>
#include "jni.h"
//#include "JNIHelp.h"
#define I2C_SLAVE	0x0703
#define SPI_CPHA		0x01
#define SPI_CPOL		0x02
struct spi_ioc_transfer {
	__u64		tx_buf;
	__u64		rx_buf;

	__u32		len;
	__u32		speed_hz;

	__u16		delay_usecs;
	__u8		bits_per_word;
	__u8		cs_change;
	__u32		pad;

	/* If the contents of 'struct spi_ioc_transfer' ever change
	 * incompatibly, then the ioctl number (currently 0) must change;
	 * ioctls with constant size fields get a bit more in the way of
	 * error checking than ones (like this) where that field varies.
	 *
	 * NOTE: struct layout is the same in 64bit and 32bit userspace.
	 */
};
#define SPI_IOC_MAGIC			'k'
#define SPI_MSGSIZE(N) \
	((((N)*(sizeof (struct spi_ioc_transfer))) < (1 << _IOC_SIZEBITS)) \
		? ((N)*(sizeof (struct spi_ioc_transfer))) : 0)
#define SPI_IOC_MESSAGE(N) _IOW(SPI_IOC_MAGIC, 0, char[SPI_MSGSIZE(N)])
#define SPI_IOC_RD_MODE			_IOR(SPI_IOC_MAGIC, 1, __u8)
#define SPI_IOC_WR_MODE			_IOW(SPI_IOC_MAGIC, 1, __u8)
#define  TAG    "A20_JNI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)
#define I2C_DEVICE_NAME		"/dev/i2c-3"
#define SPI_DEVICE_NAME		"/dev/spidev0.0"
#define TTY_DEVICE_NAME		"/dev/ttyS1"

int serialfd=-1;
JNIEXPORT jobject Java_OpenSerialPort
  (JNIEnv *env, jobject thiz)
{
	int fd;
	speed_t speed=B115200;
	jobject mFileDescriptor;

	LOGD("Opening serial port %s", TTY_DEVICE_NAME);
	fd = open(TTY_DEVICE_NAME, O_RDWR);
	serialfd=fd;
	LOGD("open() fd = %d", fd);
	if (fd == -1)
	{
		LOGE("Cannot open port");
		return NULL;
	}

	struct termios cfg;
	LOGD("Configuring serial port");
	if (tcgetattr(fd, &cfg))
	{
		LOGE("tcgetattr() failed");
		close(fd);
		return NULL;
	}

	cfmakeraw(&cfg);
	cfsetispeed(&cfg, speed);
	cfsetospeed(&cfg, speed);

	if (tcsetattr(fd, TCSANOW, &cfg))
	{
		LOGE("tcsetattr() failed");
		close(fd);
		return NULL;
	}
	jclass cFileDescriptor = (*env)->FindClass(env, "java/io/FileDescriptor");
	jmethodID iFileDescriptor = (*env)->GetMethodID(env, cFileDescriptor, "<init>", "()V");
	jfieldID descriptorID = (*env)->GetFieldID(env, cFileDescriptor, "descriptor", "I");
	mFileDescriptor = (*env)->NewObject(env, cFileDescriptor, iFileDescriptor);
	(*env)->SetIntField(env, mFileDescriptor, descriptorID, (jint)fd);
	return mFileDescriptor;
}
JNIEXPORT void Java_CloseSerialPort
  (JNIEnv *env, jobject thiz)
{
	LOGE("close(fd = %d)", serialfd);
	if(-1!=serialfd)
		close(serialfd);
}
JNIEXPORT jint JNICALL Java_Battery
	(JNIEnv *env, jobject thiz)
{
	int level=0;
	char config[1] = {0x0C};
	int addr = 0x48;
	int fd = open(I2C_DEVICE_NAME, O_RDWR);
	
	if (fd == -1)
	{
		LOGE("I2C open error");
		return 1;	
	}
	//else
	//	LOGE("I2C open %s ok",I2C_DEVICE_NAME);
	if (ioctl(fd, I2C_SLAVE, addr) < 0)
	{
		LOGE("I2C_SLAVE error");
		exit(1);
	}
	write(fd, config, 1);
	//sleep(1);

	char data[2]={0};
	if(read(fd, data, 2) != 2)
	{
		LOGE("Erorr : Input/output Erorr \n");
	}
	else 
	{
		// Convert the data
		level = (data[0] * 256 + data[1]);
		if(level > 32767)
		{
			level -= 65536;
		}

		// Output data to screen
		//LOGD("Digital value of analog input: %d \n", level);
	}

	close(fd);
	return level;
}
static void pabort(const char *s)
{
	perror(s);
	//exit(-1);
}
JNIEXPORT jbyteArray JNICALL Java_SPI
	(JNIEnv *env, jobject thiz)
{
	int ret = 0;
	int i=0;
	uint8_t mode=SPI_CPHA|SPI_CPOL;
	uint8_t mode_ori=0;
	uint8_t bits = 8;
	uint32_t speed = 12000000;
	uint16_t delay=0;

	//jbyte * olddata = (jbyte*)(*env)->GetByteArrayElements(env,send_buf, 0); 
	//jsize  oldsize = (*env)->GetArrayLength(env,send_buf); 
	//unsigned char * bytesend = (unsigned char *)olddata;
	int send_len = 2068;//(int)oldsize;
	//LOGD("send_len %d",send_len);
	//for(i=0;i<send_len;i++)
	//	LOGD("%x",bytesend[i]);
	unsigned char * bytercv = (unsigned char *)malloc(72*send_len*sizeof(unsigned char));
	memset(bytercv,0,send_len);
	
	int fd = open(SPI_DEVICE_NAME, O_RDWR);
	if (fd < 0)
	{
		LOGD("can't open device");
		return NULL;
	}

	ret = ioctl(fd, SPI_IOC_RD_MODE, &mode);
	if (ret == -1)
	{
		LOGD("can't get spi mode");
		return NULL;
	}
	else
	{
		if(mode!=mode_ori)
		{
			ret = ioctl(fd, SPI_IOC_WR_MODE, &mode);
			if (ret == -1)
			{
				LOGD("can't set spi mode");
				return NULL;
			}
		}
	}

	struct spi_ioc_transfer tr = {
		.tx_buf = (unsigned long)NULL,//(unsigned long)bytesend,
		.rx_buf = (unsigned long)bytercv,
		.len = send_len,
		.delay_usecs = delay,
		.speed_hz = speed,
		.bits_per_word = bits,
	};
	for(i=0;i<72;i++)
	{

		ret = ioctl(fd, SPI_IOC_MESSAGE(1), &tr);
		if (ret < 1)
		{
			LOGD("can't send spi message");
			return NULL;
		}
		tr.rx_buf=(unsigned long)bytercv+send_len*(i+1);
	}
	close(fd);
	jbyte *by = (jbyte*)bytercv; 
	jbyteArray jarray = (*env)->NewByteArray(env,send_len*72); 
	(*env)->SetByteArrayRegion(env,jarray, 0, send_len, by);
	free(bytercv);
	//(*env)->ReleaseByteArrayElements(env,send_buf,olddata,0);
	return jarray;
#if 0
	/*
	 * bits per word
	 */
	ret = ioctl(fd, SPI_IOC_WR_BITS_PER_WORD, &bits);
	if (ret == -1)
		pabort("can't set bits per word");

	ret = ioctl(fd, SPI_IOC_RD_BITS_PER_WORD, &bits);
	if (ret == -1)
		pabort("can't get bits per word");

	/*
	 * max speed hz
	 */
	ret = ioctl(fd, SPI_IOC_WR_MAX_SPEED_HZ, &speed);
	if (ret == -1)
		pabort("can't set max speed hz");

	ret = ioctl(fd, SPI_IOC_RD_MAX_SPEED_HZ, &speed);
	if (ret == -1)
		pabort("can't get max speed hz");
#endif
}

static JNINativeMethod gMethods[] = {  
	{"wrSPI", "()[B", (void *)Java_SPI}, 
	{"getBattery", "()I", (void *)Java_Battery},
	{"closeSerial", "()V", (void *)Java_CloseSerialPort},
	{"openSerial", "()Ljava/io/FileDescriptor;", (void *)Java_OpenSerialPort}
}; 
 
static int register_android_realarm_test(JNIEnv *env)  
{  
   	jclass clazz;
    static const char* const kClassName =  "com/example/a20_prj/HardwareControl";

    clazz = (*env)->FindClass(env, kClassName);
    if (clazz == NULL) {
        LOGE("Can't find class %s\n", kClassName);
        return -1;
    }
    if ((*env)->RegisterNatives(env,clazz, gMethods, sizeof(gMethods) / sizeof(gMethods[0])) != JNI_OK)
    {
        LOGE("Failed registering methods for %s\n", kClassName);
        return -1;
    }

    return 0;
}
jint JNI_OnLoad(JavaVM* vm, void* reserved) 
{
    
	JNIEnv *env = NULL;
	if ((*vm)->GetEnv(vm,(void**) &env, JNI_VERSION_1_4) != JNI_OK) {  
		LOGI("Error GetEnv\n");  
		return -1;  
	} 
	assert(env != NULL);  
	if (register_android_realarm_test(env) < 0)
	{
		LOGE("register_android_realarm_test error."); 
		return -1;  
	}
	LOGI("/*****************A20 Hardware Load**********************/");

    return JNI_VERSION_1_4;
}

