
/* Handle clipboard text and data in arbitrary formats */

/* Miscellaneous defines */
#define TextScrap(A, B, C, D) (int)((A<<24)|(B<<16)|(C<<8)|(D<<0))

extern int init_scrap();
extern int lost_scrap();
extern void put_scrap(int type, int srclen, String src);
extern void get_scrap(int type, int *dstlen, char **dst);
