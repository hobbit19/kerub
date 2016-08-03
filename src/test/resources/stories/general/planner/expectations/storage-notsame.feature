Feature: support for not-same-storage expectation

  Scenario: When a not-same-storage virtual disk is allocated on host, allocate storage on another host
	Given hosts:
	  | address           | ram  | Cores | Threads | Architecture | Operating System |
	  | host1.example.com | 8 GB | 2     | 4       | x86_64       | Linux            |
	  | host2.example.com | 8 GB | 2     | 4       | x86_64       | Linux            |
	And host host1.example.com filesystem is:
	  | mount point | size   | free   |
	  | /var        | 128 GB | 128 GB |
	And host host2.example.com filesystem is:
	  | mount point | size   | free   |
	  | /var        | 128 GB | 128 GB |
	And virtual storage devices:
	  | name     | size | ro    |
	  | vm1-disk | 2 GB | false |
	  | vm2-disk | 2 GB | false |
	And vm1-disk is not yet created
	And the virtual disk vm2-disk is created on host2.example.com
	And host host1.example.com is Up
	And host host2.example.com is Up
	And virtual disk vm1-disk has not-same-storage expectation against vm2-disk
	When virtual disk vm1-disk gets an availability expectation
	Then the virtual disk vm1-disk must be allocated on host1.example.com under /var
