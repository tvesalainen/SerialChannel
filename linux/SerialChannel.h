/*
 * Copyright (C) 2011 Timo Vesalainen
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

#include <sys/types.h>
#include <sys/stat.h>
#include <sys/uio.h>
#include <fcntl.h>
#include <termios.h>
#include <stdio.h>
#include <string.h>
#include <unistd.h>
#include <dirent.h>
#include <errno.h>
#include <malloc.h>
#include <pthread.h>
#include <signal.h>
#include <sys/select.h>
#include "org_vesalainen_comm_channel_LinuxSerialChannel.h"

typedef struct _CTX
{
    int fd;
    char szPort[PATH_MAX];
    struct termios oldtio;
    struct termios newtio;
} CTX;
void exception(JNIEnv * env, const char* clazz, const char* message);
char* configure(
	JNIEnv *env, 
	CTX *c, 
	int bauds, 
	int parity, 
	int databits, 
	int stopbits, 
	int flow
	);
