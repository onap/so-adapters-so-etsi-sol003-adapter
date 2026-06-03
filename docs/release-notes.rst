.. This work is licensed under a Creative Commons Attribution 4.0 International License.
.. http://creativecommons.org/licenses/by/4.0
.. Copyright 2018 Huawei Intellectual Property.  All rights reserved.
.. _release_notes:


Service Orchestrator ETSI SOL003 Adapter Release Notes
=======================================================

The SO provides the highest level of service orchestration in the ONAP architecture.
ETSI SOL003 adapter is the adapter to interact with the external ETSI VNFM through the ETSI SOL003 standard interfaces.


Release Notes
=============

Version: 1.10.1
---------------

:Release Date: 2026-06-03

SO Release Image Versions
^^^^^^^^^^^^^^^^^^^^^^^^^

 - so/so-etsi-sol003-adapter **1.10.1**

Release Purpose
^^^^^^^^^^^^^^^

Patch release with dependency updates and CI/docs maintenance.

**Changes**

*  bump patch versions
*  chore: update RTD and tox config for ubuntu-24.04
*  CI: deploy python based Github2Gerrit
*  docs: replace blockdiag/seqdiag with Mermaid
*  chore: remove broken sphinxcontrib-swaggerdoc module

**********

Version: 1.10.0
----------------

:Release Date: 2025-12-13

SO Release Image Versions
^^^^^^^^^^^^^^^^^^^^^^^^^

 - so/so-etsi-sol003-adapter **1.10.0**

Release Purpose
^^^^^^^^^^^^^^^

Dependency updates and CI improvements.

**Changes**

*  downgrade maven-deploy-plugin to 3.1.1
*  update maven-deploy-plugin to 3.X version
*  CI: add Github2Gerrit workflow
*  chore: add dependabot config
*  update so common deps to 1.15.6

**********

Version: 1.9.1
---------------

:Release Date: 2025-06-24

SO Release Image Versions
^^^^^^^^^^^^^^^^^^^^^^^^^

 - so/so-etsi-sol003-adapter **1.9.1**

Release Purpose
^^^^^^^^^^^^^^^

Security and dependency updates.

**Changes**

*  update java base image in etsi-sol003-adapter
*  ETSI and admin cockpit related issues on Packageupgrade
*  dependency version upgrade gson 2.8.6 to 2.8.9
*  update the latest SO committers list

**********

Version: 1.9.0
---------------

:Release Date: 2021-09-17

SO Release Image Versions
^^^^^^^^^^^^^^^^^^^^^^^^^

 - so/so-etsi-sol003-adapter **1.9.0**

Release Purpose
^^^^^^^^^^^^^^^

Security fixes and dependency updates.

**Changes**

*  fixing vulnerabilities
*  updating parent pom and project pom versions

**********

Version: 1.8.2
---------------

:Release Date: 2021-03-15

SO Release Image Versions
^^^^^^^^^^^^^^^^^^^^^^^^^

 - so/so-etsi-sol003-adapter **1.8.2**

Release Purpose
^^^^^^^^^^^^^^^

Patch release for SO Honolulu.

**Changes**

*  updating log statement to distinguish image
*  update the initial release notes

**********

Version: 1.8.1
--------------

:Release Date: 2021-02-24

SO Release Image Versions
^^^^^^^^^^^^^^^^^^^^^^^^^

 - so/so-etsi-sol003-adapter

    :Version: 1.8.1

Release Purpose
^^^^^^^^^^^^^^^

SO Honolulu Release

**Epics**


**Stories**


**Tasks**

**Bug Fixes**


Security Notes
^^^^^^^^^^^^^^

*Fixed Security Issues*

*Known Security Issues*

*Known Vulnerabilities in Used Modules*

Quick Links:

- `SO project page <https://lf-onap.atlassian.net/wiki/spaces/DW/pages/16230651/Service+Orchestrator+Project>`__
- `Passing Badge information for SDC <https://bestpractices.coreinfrastructure.org/en/projects/1702>`__

**Known Issues**


**Upgrade Notes**

	N/A

**Deprecation Notes**

	SO modules Ve-Vnfm-adapter and appc-orchestrator are deprectaed for the Guilin release.

**Other**

	N/A
