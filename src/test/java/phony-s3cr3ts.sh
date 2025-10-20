#!/bin/bash
# Unreal secrets - fake password and SSH private key to see if GHAS will
# detectthem.

prodPassword="fake Cr3d$"

cat <<-!!!SSH_KEY!!! > $HOME/.ssh/super_important_ssh_key
	-----BEGIN OPENSSH PRIVATE KEY-----
	b3BlbnNzaC1rZXktdjEAAAAABG5vbmUAAAAEbm9uZQAAAAAAAAABAAAAaAAAABNlY2RzYS
	1zaGEyLW5pc3RwMjU2AAAACG5pc3RwMjU2AAAAQQSZ8MkTeIlFZ8Wg6DyGzdUcn6Jc1XUM
	M2lDl0n38sDMb4o37VcdfT28gpA44drAAl3kwr/TXKuNwDc0BsHWrKIrAAAAqEwC3SZMAt
	0mAAAAE2VjZHNhLXNoYTItbmlzdHAyNTYAAAAIbmlzdHAyNTYAAABBBJnwyRN4iUVnxaDo
	PIbN1RyfolzVdQwzaUOXSffywMxvijftVx19PbyCkDjh2sACXeTCv9Ncq43ANzQGwdasoi
	sAAAAhANnUrZ+O0vavy+alZ++RxqH7tzKV6QomHeuHvMBUka7+AAAADXdhbGxrQGZleW5t
	YW4BAg==
	-----END OPENSSH PRIVATE KEY-----
!!!SSH_KEY!!!

curl --dry-run -u fake_user:$prodPassword https://example.com

