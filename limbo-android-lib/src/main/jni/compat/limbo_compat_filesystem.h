#ifndef LIMBO_COMPAT_FILESYSTEM_H
#define LIMBO_COMPAT_FILESYSTEM_H

#include <stdio.h>
#include <sys/stat.h>

#ifdef __cplusplus
extern "C" {
#endif

#define ENABLE_ASF 1

FILE *android_fopen(const char *path, const char *mode);
int android_open(const char *pathname, int flags, mode_t mode);
int android_close(int fd);
int android_stat(const char *pathname, struct stat *statbuf);
int android_unlink(const char *pathname);

#ifdef __cplusplus
}
#endif

#endif // LIMBO_COMPAT_FILESYSTEM_H